package com.minicloud.infrastructure.host;

import com.minicloud.config.MiniCloudProperties;
import com.minicloud.infrastructure.process.CommandResult;
import com.minicloud.infrastructure.process.ControlledProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import com.sun.management.OperatingSystemMXBean;

@Component
public class LinuxHostMetricsProvider implements HostMetricsProvider {

    private static final Logger log = LoggerFactory.getLogger(LinuxHostMetricsProvider.class);
    private static final Path OS_RELEASE = Path.of("/etc/os-release");
    private static final Path CPUINFO = Path.of("/proc/cpuinfo");
    private static final Path MEMINFO = Path.of("/proc/meminfo");
    private static final Path LOADAVG = Path.of("/proc/loadavg");
    private static final Path KERNEL_RELEASE = Path.of("/proc/sys/kernel/osrelease");
    private static final Path KVM_DEVICE = Path.of("/dev/kvm");
    private static final Path KVM_MODULE = Path.of("/sys/module/kvm");
    private static final List<Path> LIBVIRT_SOCKETS = List.of(
            Path.of("/run/libvirt/libvirt-sock"),
            Path.of("/var/run/libvirt/libvirt-sock")
    );

    private final MiniCloudProperties properties;

    public LinuxHostMetricsProvider(MiniCloudProperties properties) {
        this.properties = properties;
    }

    @Override
    public HostSnapshot collect() {
        Instant now = Instant.now();
        MemoryReading memory = readMemory();
        DiskReading disk = readDisk();
        CpuFlags cpuFlags = readCpuFlags();
        return new HostSnapshot(
                readHostname(),
                readOsName(),
                readOsVersion(),
                readKernel(),
                System.getProperty("os.arch"),
                Runtime.getRuntime().availableProcessors(),
                cpuFlags.model(),
                readCpuUsagePercent(),
                readLoadAverage(),
                memory.totalBytes(),
                memory.availableBytes(),
                disk.totalBytes(),
                disk.availableBytes(),
                disk.mount(),
                detectKvm(cpuFlags),
                detectLibvirt(),
                now
        );
    }

    private String readHostname() {
        try {
            Path hostnameFile = Path.of("/etc/hostname");
            if (Files.exists(hostnameFile)) {
                String value = Files.readString(hostnameFile).trim();
                if (!value.isBlank()) {
                    return value;
                }
            }
            return InetAddress.getLocalHost().getHostName();
        } catch (IOException ex) {
            log.debug("Could not read hostname: {}", ex.getMessage());
            return "unknown-host";
        }
    }

    private String readOsName() {
        return readOsReleaseField("NAME").orElse(System.getProperty("os.name"));
    }

    private String readOsVersion() {
        return readOsReleaseField("PRETTY_NAME")
                .or(() -> readOsReleaseField("VERSION"))
                .orElse(System.getProperty("os.version"));
    }

    private Optional<String> readOsReleaseField(String key) {
        if (!Files.exists(OS_RELEASE)) {
            return Optional.empty();
        }
        try {
            return Files.readAllLines(OS_RELEASE).stream()
                    .filter(line -> line.startsWith(key + "="))
                    .map(line -> line.substring(key.length() + 1).replace("\"", "").trim())
                    .findFirst();
        } catch (IOException ex) {
            log.debug("Could not read /etc/os-release: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    private String readKernel() {
        try {
            if (Files.exists(KERNEL_RELEASE)) {
                return Files.readString(KERNEL_RELEASE).trim();
            }
        } catch (IOException ex) {
            log.debug("Could not read kernel release file: {}", ex.getMessage());
        }
        CommandResult result = ControlledProcess.run(3, "uname", "-r");
        if (result.succeeded() && !result.stdout().isBlank()) {
            return result.stdout();
        }
        return System.getProperty("os.version");
    }

    private Double readCpuUsagePercent() {
        try {
            OperatingSystemMXBean os = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
            double load = os.getCpuLoad();
            if (load >= 0) {
                return load * 100.0;
            }
        } catch (Exception ex) {
            log.debug("OperatingSystemMXBean CPU load unavailable: {}", ex.getMessage());
        }
        return null;
    }

    private Double readLoadAverage() {
        try {
            if (Files.exists(LOADAVG)) {
                String first = Files.readString(LOADAVG).trim().split("\\s+")[0];
                return Double.parseDouble(first);
            }
        } catch (IOException | NumberFormatException ex) {
            log.debug("Could not read load average: {}", ex.getMessage());
        }
        double systemLoad = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
        return systemLoad >= 0 ? systemLoad : null;
    }

    private MemoryReading readMemory() {
        if (Files.exists(MEMINFO)) {
            try {
                Long total = null;
                Long available = null;
                for (String line : Files.readAllLines(MEMINFO)) {
                    if (line.startsWith("MemTotal:")) {
                        total = parseKbLine(line);
                    } else if (line.startsWith("MemAvailable:")) {
                        available = parseKbLine(line);
                    }
                }
                if (total != null) {
                    return new MemoryReading(total, available);
                }
            } catch (IOException ex) {
                log.debug("Could not read /proc/meminfo: {}", ex.getMessage());
            }
        }
        try {
            OperatingSystemMXBean os = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
            return new MemoryReading(os.getTotalMemorySize(), os.getFreeMemorySize());
        } catch (Exception ex) {
            log.warn("Memory metrics unavailable");
            return new MemoryReading(null, null);
        }
    }

    private Long parseKbLine(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 2) {
            return null;
        }
        return Long.parseLong(parts[1]) * 1024L;
    }

    private DiskReading readDisk() {
        try {
            var store = Files.getFileStore(Path.of("/"));
            return new DiskReading(store.getTotalSpace(), store.getUsableSpace(), "/");
        } catch (IOException ex) {
            log.warn("Root filesystem metrics unavailable: {}", ex.getMessage());
            return new DiskReading(null, null, "/");
        }
    }

    private CpuFlags readCpuFlags() {
        if (!Files.exists(CPUINFO)) {
            return new CpuFlags(null, false, false);
        }
        try {
            String model = null;
            boolean vmx = false;
            boolean svm = false;
            for (String line : Files.readAllLines(CPUINFO)) {
                if (model == null && line.startsWith("model name")) {
                    int idx = line.indexOf(':');
                    if (idx >= 0) {
                        model = line.substring(idx + 1).trim();
                    }
                }
                if (line.startsWith("flags") || line.startsWith("Features")) {
                    int idx = line.indexOf(':');
                    String flags = (idx >= 0 ? line.substring(idx + 1) : line).toLowerCase(Locale.ROOT);
                    for (String flag : flags.trim().split("\\s+")) {
                        if ("vmx".equals(flag)) {
                            vmx = true;
                        }
                        if ("svm".equals(flag)) {
                            svm = true;
                        }
                    }
                }
            }
            return new CpuFlags(model, vmx, svm);
        } catch (IOException ex) {
            log.debug("Could not read /proc/cpuinfo: {}", ex.getMessage());
            return new CpuFlags(null, false, false);
        }
    }

    private HostSnapshot.KvmDetection detectKvm(CpuFlags cpuFlags) {
        boolean devicePresent = Files.exists(KVM_DEVICE);
        boolean moduleLoaded = Files.exists(KVM_MODULE);
        boolean hardware = cpuFlags.vmx() || cpuFlags.svm();
        String vendor = cpuFlags.vmx() ? "intel-vt-x" : cpuFlags.svm() ? "amd-v" : "none";
        String status;
        String detail;
        if (devicePresent && hardware) {
            status = "AVAILABLE";
            detail = "/dev/kvm is present and the CPU advertises hardware virtualization (" + vendor + ")";
        } else if (hardware && !devicePresent) {
            status = "UNAVAILABLE";
            detail = "CPU supports virtualization (" + vendor + ") but /dev/kvm is missing. Install qemu-kvm and load the kvm module.";
        } else if (devicePresent) {
            status = "AVAILABLE";
            detail = "/dev/kvm is present. CPU flags for vmx/svm were not detected in /proc/cpuinfo.";
        } else {
            status = "UNAVAILABLE";
            detail = "KVM is not available. /dev/kvm is missing and hardware virtualization flags were not found.";
        }
        return new HostSnapshot.KvmDetection(devicePresent, moduleLoaded, hardware, vendor, status, detail);
    }

    private HostSnapshot.LibvirtDetection detectLibvirt() {
        String uri = properties.getHypervisor().getUri();
        Path socket = LIBVIRT_SOCKETS.stream().filter(Files::exists).findFirst().orElse(null);
        boolean socketPresent = socket != null;
        CommandResult service = ControlledProcess.run(3, "systemctl", "is-active", "libvirtd");
        String serviceState = service.succeeded() || !service.stdout().isBlank() ? service.stdout() : "unknown";
        if (service.timedOut() || service.stdout().isBlank()) {
            if (service.stderr() != null && service.stderr().toLowerCase(Locale.ROOT).contains("not found")) {
                serviceState = "not-installed";
            } else if (!socketPresent) {
                serviceState = "not-detected";
            }
        }

        CommandResult virshWhich = ControlledProcess.run(3, "virsh", "--version");
        boolean virshAvailable = virshWhich.succeeded();

        boolean connected = false;
        String detail;
        String status;
        if (virshAvailable) {
            CommandResult connect = ControlledProcess.run(5, "virsh", "--connect", uri, "version");
            connected = connect.succeeded();
            if (connected) {
                status = "CONNECTED";
                detail = "Connected to " + uri + " using virsh.";
            } else if (containsIgnoreCase(connect.stderr(), "permission") || containsIgnoreCase(connect.stdout(), "permission")) {
                status = "PERMISSION_DENIED";
                detail = "libvirt refused the connection. Add this user to the libvirt group and re-login.";
            } else {
                status = socketPresent ? "SOCKET_PRESENT" : "UNAVAILABLE";
                detail = firstNonBlank(connect.stderr(), connect.stdout(), "virsh could not connect to " + uri);
            }
        } else if (socketPresent) {
            status = "SOCKET_PRESENT";
            detail = "libvirt socket exists at " + socket + " but virsh is not installed or not on PATH.";
        } else {
            status = "NOT_INSTALLED";
            detail = "libvirt was not detected. Install qemu-kvm, libvirt-daemon-system, and libvirt-clients.";
        }

        return new HostSnapshot.LibvirtDetection(
                socketPresent,
                socket == null ? null : socket.toString(),
                serviceState,
                virshAvailable,
                connected,
                uri,
                status,
                detail
        );
    }

    private boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "Unavailable";
    }

    private record MemoryReading(Long totalBytes, Long availableBytes) {
    }

    private record DiskReading(Long totalBytes, Long availableBytes, String mount) {
    }

    private record CpuFlags(String model, boolean vmx, boolean svm) {
    }
}

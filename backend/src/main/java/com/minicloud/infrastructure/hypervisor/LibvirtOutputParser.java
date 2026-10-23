package com.minicloud.infrastructure.hypervisor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parses controlled virsh text output. Kept separate so tests do not need KVM.
 */
public final class LibvirtOutputParser {

    private LibvirtOutputParser() {
    }

    public static List<VirtualMachineInfo> parseList(String stdout) {
        List<VirtualMachineInfo> vms = new ArrayList<>();
        if (stdout == null || stdout.isBlank()) {
            return vms;
        }
        for (String rawLine : stdout.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("Id") || isSeparator(line)) {
                continue;
            }
            String[] parts = line.split("\\s+", 3);
            if (parts.length < 3) {
                continue;
            }
            String id = "-".equals(parts[0]) ? null : parts[0];
            String name = parts[1];
            if (!LibvirtNames.isSafe(name)) {
                continue;
            }
            String state = parts[2].trim();
            vms.add(VirtualMachineInfo.summary(name, id, state, normalizeState(state)));
        }
        return vms;
    }

    public static VirtualMachineInfo mergeDominfo(VirtualMachineInfo base, String stdout) {
        Map<String, String> fields = parseKeyed(stdout);
        String state = orDefault(fields, "State", base.state());
        return new VirtualMachineInfo(
                orDefault(fields, "Name", base.name()),
                fields.get("UUID"),
                blankToNull(fields.get("Id")),
                state,
                normalizeState(state),
                parseInt(fields.get("CPU(s)")),
                kibToBytes(lookup(fields, "Max memory", "Max Memory")),
                kibToBytes(lookup(fields, "Used memory", "Used Memory")),
                fields.get("OS Type"),
                parseYesNo(fields.get("Persistent")),
                parseYesNo(fields.get("Autostart")),
                base.disks(),
                base.nics()
        );
    }

    public static List<VmDiskInfo> parseBlkList(String stdout) {
        List<VmDiskInfo> disks = new ArrayList<>();
        if (stdout == null || stdout.isBlank()) {
            return disks;
        }
        for (String rawLine : stdout.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("Type") || isSeparator(line)) {
                continue;
            }
            String[] parts = line.split("\\s+", 4);
            if (parts.length < 3) {
                continue;
            }
            String source = parts.length > 3 ? parts[3] : null;
            if ("-".equals(source)) {
                source = null;
            }
            disks.add(new VmDiskInfo(parts[1], parts[0], parts[2], source));
        }
        return disks;
    }

    public static List<VmNicInfo> parseIfList(String stdout) {
        List<VmNicInfo> nics = new ArrayList<>();
        if (stdout == null || stdout.isBlank()) {
            return nics;
        }
        for (String rawLine : stdout.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("Interface") || isSeparator(line)) {
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length < 5) {
                continue;
            }
            String hostInterface = "-".equals(parts[0]) ? null : parts[0];
            nics.add(new VmNicInfo(parts[0], parts[1], parts[2], parts[3], parts[4], null, null, hostInterface));
        }
        return nics;
    }

    public static List<VmNicInfo> applyAddresses(List<VmNicInfo> nics, String stdout) {
        if (nics.isEmpty() || stdout == null || stdout.isBlank()) {
            return nics;
        }
        Map<String, String> ipByMac = new LinkedHashMap<>();
        Map<String, String> ipByName = new LinkedHashMap<>();
        for (String rawLine : stdout.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("Name") || isSeparator(line)) {
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length < 4) {
                continue;
            }
            ipByName.put(parts[0], parts[3]);
            ipByMac.put(parts[1].toLowerCase(Locale.ROOT), parts[3]);
        }
        return nics.stream()
                .map(nic -> {
                    String ip = nic.mac() != null ? ipByMac.get(nic.mac().toLowerCase(Locale.ROOT)) : null;
                    if (ip == null) {
                        ip = ipByName.get(nic.interfaceName());
                    }
                    return nic.withIp(ip);
                })
                .toList();
    }

    public static LibvirtNodeInfo parseNodeinfo(String stdout) {
        Map<String, String> fields = parseKeyed(stdout);
        return new LibvirtNodeInfo(
                fields.get("CPU model"),
                parseInt(fields.get("CPU(s)")),
                parseInt(fields.get("CPU socket(s)")),
                parseInt(fields.get("Core(s) per socket")),
                parseInt(fields.get("Thread(s) per core")),
                kibToBytes(fields.get("Memory size"))
        );
    }

    public static LibvirtVersionInfo parseVersion(String stdout) {
        if (stdout == null || stdout.isBlank()) {
            return new LibvirtVersionInfo(null, null, stdout);
        }
        String libvirt = null;
        String hypervisor = null;
        for (String rawLine : stdout.split("\\R")) {
            String line = rawLine.trim();
            if (line.startsWith("Using library:")) {
                libvirt = line.substring("Using library:".length()).trim();
            } else if (line.startsWith("Running hypervisor:")) {
                hypervisor = line.substring("Running hypervisor:".length()).trim();
            }
        }
        return new LibvirtVersionInfo(libvirt, hypervisor, stdout);
    }

    public static String normalizeState(String state) {
        if (state == null) {
            return "UNKNOWN";
        }
        String value = state.toLowerCase(Locale.ROOT).replace(" ", "");
        return switch (value) {
            case "running" -> "RUNNING";
            case "shutoff", "shutdown", "inshutdown" -> "STOPPED";
            case "paused" -> "PAUSED";
            case "crashed", "pmsuspended" -> "ERROR";
            default -> "UNKNOWN";
        };
    }

    public static List<StoragePoolInfo> parsePoolList(String stdout) {
        List<StoragePoolInfo> pools = new ArrayList<>();
        if (stdout == null || stdout.isBlank()) {
            return pools;
        }
        for (String rawLine : stdout.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("Name") || isSeparator(line)) {
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length < 2 || !LibvirtNames.isSafe(parts[0])) {
                continue;
            }
            pools.add(new StoragePoolInfo(parts[0], parts[1], null, null, null));
        }
        return pools;
    }

    public static List<VolumeSummary> parseVolList(String stdout) {
        List<VolumeSummary> volumes = new ArrayList<>();
        if (stdout == null || stdout.isBlank()) {
            return volumes;
        }
        for (String rawLine : stdout.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("Name") || isSeparator(line)) {
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length < 7 || !LibvirtNames.isSafe(parts[0])) {
                continue;
            }
            String name = parts[0];
            String type = parts[parts.length - 5];
            Long capacity = parseHumanSize(parts[parts.length - 4] + " " + parts[parts.length - 3]);
            Long allocation = parseHumanSize(parts[parts.length - 2] + " " + parts[parts.length - 1]);
            String path = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length - 5));
            volumes.add(new VolumeSummary(name, path, type, capacity, allocation));
        }
        return volumes;
    }

    public record VolumeSummary(String name, String path, String type, Long capacityBytes, Long allocationBytes) {
    }

    public static boolean looksLikeMissingPool(CommandHint hint) {
        String text = (safe(hint.stdout()) + " " + safe(hint.stderr())).toLowerCase(Locale.ROOT);
        return text.contains("failed to get pool")
                || text.contains("storage pool not found")
                || text.contains("no storage pool");
    }

    public static StoragePoolInfo parsePoolInfo(String name, String stdout) {
        Map<String, String> fields = parseKeyed(stdout);
        return new StoragePoolInfo(
                orDefault(fields, "Name", name),
                fields.get("State"),
                parseHumanSize(fields.get("Capacity")),
                parseHumanSize(fields.get("Allocation")),
                parseHumanSize(fields.get("Available"))
        );
    }

    public static List<VmNicInfo> applyDhcpLeases(List<VmNicInfo> nics, String stdout) {
        if (nics.isEmpty() || stdout == null || stdout.isBlank()) {
            return nics;
        }
        Map<String, String> ipByMac = parseDhcpLeases(stdout);
        if (ipByMac.isEmpty()) {
            return nics;
        }
        return nics.stream()
                .map(nic -> {
                    if (nic.ip() != null || nic.mac() == null) {
                        return nic;
                    }
                    String ip = ipByMac.get(nic.mac().toLowerCase(Locale.ROOT));
                    return ip == null ? nic : nic.withIp(ip);
                })
                .toList();
    }

    public static Map<String, String> parseDhcpLeases(String stdout) {
        Map<String, String> ipByMac = new LinkedHashMap<>();
        if (stdout == null || stdout.isBlank()) {
            return ipByMac;
        }
        for (String rawLine : stdout.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("Expiry") || isSeparator(line)) {
                continue;
            }
            java.util.regex.Matcher mac = java.util.regex.Pattern.compile("(?i)(?:[0-9a-f]{2}:){5}[0-9a-f]{2}").matcher(line);
            java.util.regex.Matcher ip = java.util.regex.Pattern.compile("\\b(\\d{1,3}(?:\\.\\d{1,3}){3})(?:/\\d+)?\\b").matcher(line);
            if (mac.find() && ip.find()) {
                ipByMac.put(mac.group().toLowerCase(Locale.ROOT), ip.group());
            }
        }
        return ipByMac;
    }

    public static boolean looksLikeMissingNetwork(CommandHint hint) {
        String text = (safe(hint.stdout()) + " " + safe(hint.stderr())).toLowerCase(Locale.ROOT);
        return text.contains("failed to get network")
                || text.contains("network not found")
                || text.contains("no network with matching name");
    }

    public static List<NetworkSummary> parseNetList(String stdout) {
        List<NetworkSummary> networks = new ArrayList<>();
        if (stdout == null || stdout.isBlank()) {
            return networks;
        }
        for (String rawLine : stdout.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("Name") || isSeparator(line)) {
                continue;
            }
            String[] parts = line.split("\\s+");
            if (parts.length < 2 || !LibvirtNames.isSafe(parts[0])) {
                continue;
            }
            boolean autostart = parts.length > 2 && parts[2].equalsIgnoreCase("yes");
            networks.add(new NetworkSummary(parts[0], parts[1], autostart));
        }
        return networks;
    }

    public static Long parseHumanSize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String[] parts = value.trim().split("\\s+");
        try {
            double amount = Double.parseDouble(parts[0]);
            String unit = parts.length > 1 ? parts[1].toLowerCase(Locale.ROOT) : "b";
            double multiplier = switch (unit) {
                case "kib", "kb" -> 1024d;
                case "mib", "mb" -> 1024d * 1024d;
                case "gib", "gb" -> 1024d * 1024d * 1024d;
                case "tib", "tb" -> 1024d * 1024d * 1024d * 1024d;
                default -> 1d;
            };
            return Math.round(amount * multiplier);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static boolean looksLikeMissingDomain(CommandHint hint) {
        String text = (safe(hint.stdout()) + " " + safe(hint.stderr())).toLowerCase(Locale.ROOT);
        return text.contains("failed to get domain")
                || text.contains("domain not found")
                || text.contains("no domain with matching name");
    }

    public record CommandHint(String stdout, String stderr) {
    }

    public static String parseKeyedField(String stdout, String key) {
        return parseKeyed(stdout).get(key);
    }

    private static Map<String, String> parseKeyed(String stdout) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (stdout == null) {
            return fields;
        }
        for (String rawLine : stdout.split("\\R")) {
            int idx = rawLine.indexOf(':');
            if (idx <= 0) {
                continue;
            }
            String key = rawLine.substring(0, idx).trim();
            String value = rawLine.substring(idx + 1).trim();
            if (!key.isEmpty()) {
                fields.put(key, value);
            }
        }
        return fields;
    }

    private static boolean isSeparator(String line) {
        return line.chars().allMatch(ch -> ch == '-' || ch == ' ');
    }

    private static String lookup(Map<String, String> fields, String... keys) {
        for (String key : keys) {
            String value = fields.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String orDefault(Map<String, String> fields, String key, String fallback) {
        String value = fields.get(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static Integer parseInt(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value.replaceAll("[^0-9-]", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Long kibToBytes(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isEmpty()) {
            return null;
        }
        return Long.parseLong(digits) * 1024L;
    }

    public record VncDisplay(String host, int display, int port) {
    }

    /**
     * {@code virsh vncdisplay} prints {@code 127.0.0.1:0} meaning display 0 → TCP 5900.
     * A number ≥ 5900 after the colon is treated as a raw TCP port.
     */
    public static VncDisplay parseVncDisplay(String stdout) {
        if (stdout == null || stdout.isBlank()) {
            return null;
        }
        String line = stdout.trim().split("\\R", 2)[0].trim();
        if (line.isEmpty()) {
            return null;
        }
        int colon = line.lastIndexOf(':');
        if (colon < 0) {
            return null;
        }
        String host = colon == 0 ? "127.0.0.1" : line.substring(0, colon).trim();
        if (host.isEmpty()) {
            host = "127.0.0.1";
        }
        try {
            int value = Integer.parseInt(line.substring(colon + 1).trim());
            if (value >= 5900) {
                return new VncDisplay(host, value - 5900, value);
            }
            if (value < 0 || value > 99) {
                return null;
            }
            return new VncDisplay(host, value, 5900 + value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static Boolean parseYesNo(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("yes") || normalized.startsWith("enable")) {
            return true;
        }
        if (normalized.startsWith("no") || normalized.startsWith("disable")) {
            return false;
        }
        return null;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank() || "-".equals(value)) {
            return null;
        }
        return value;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

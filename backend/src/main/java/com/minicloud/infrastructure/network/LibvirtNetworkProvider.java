package com.minicloud.infrastructure.network;

import com.minicloud.common.exception.InfrastructureConnectionException;
import com.minicloud.common.exception.NetworkAlreadyExistsException;
import com.minicloud.common.exception.NetworkInUseException;
import com.minicloud.common.exception.NetworkNotFoundException;
import com.minicloud.common.exception.NetworkOperationException;
import com.minicloud.infrastructure.host.HostMetricsProvider;
import com.minicloud.infrastructure.hypervisor.LibvirtCli;
import com.minicloud.infrastructure.hypervisor.LibvirtNames;
import com.minicloud.infrastructure.hypervisor.LibvirtOutputParser;
import com.minicloud.infrastructure.hypervisor.NetworkSummary;
import com.minicloud.infrastructure.process.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Talks to libvirt virtual networks. libvirt is the source of truth.
 */
@Component
public class LibvirtNetworkProvider implements NetworkProvider {

    private static final Logger log = LoggerFactory.getLogger(LibvirtNetworkProvider.class);

    private final HostMetricsProvider hostMetricsProvider;
    private final LibvirtCli libvirtCli;

    public LibvirtNetworkProvider(HostMetricsProvider hostMetricsProvider, LibvirtCli libvirtCli) {
        this.hostMetricsProvider = hostMetricsProvider;
        this.libvirtCli = libvirtCli;
    }

    @Override
    public List<VirtualNetworkInfo> listNetworks() {
        if (!connected()) {
            log.info("Skipping network listing: libvirt is not connected");
            return List.of();
        }
        CommandResult result = libvirtCli.netList();
        if (!result.succeeded()) {
            log.warn("virsh net-list failed: {}", firstNonBlank(result.stderr(), result.stdout()));
            return List.of();
        }
        List<VirtualNetworkInfo> networks = new ArrayList<>();
        for (NetworkSummary summary : LibvirtOutputParser.parseNetList(result.stdout())) {
            networks.add(describe(summary.name()));
        }
        log.info("libvirt reported {} network(s)", networks.size());
        return networks;
    }

    @Override
    public VirtualNetworkInfo getNetwork(String name) {
        requireConnected();
        LibvirtNames.requireSafe(name, "network");
        return describe(name);
    }

    @Override
    public VirtualNetworkInfo createNetwork(CreateNetworkRequest request) {
        requireConnected();
        String name = LibvirtNames.requireSafe(request.name(), "network");
        if (exists(name)) {
            throw new NetworkAlreadyExistsException(name);
        }
        Ipv4Networks.Block block = Ipv4Networks.parseCidr(request.cidr());
        Ipv4Networks.validateCreate(request.cidr(), request.gateway(), request.dhcpStart(), request.dhcpEnd());
        String xml = NetworkXmlFactory.natNetwork(
                name,
                request.gateway(),
                block.netmaskString(),
                request.dhcpStart(),
                request.dhcpEnd()
        );
        log.info("Defining libvirt network {} cidr={} gateway={}", name, request.cidr(), request.gateway());
        CommandResult define = libvirtCli.netDefine(xml);
        if (!define.succeeded()) {
            throw new NetworkOperationException("libvirt net-define failed: " + firstNonBlank(define.stderr(), define.stdout()));
        }
        CommandResult start = libvirtCli.netStart(name);
        if (!start.succeeded()) {
            throw new NetworkOperationException("Network was defined but start failed: "
                    + firstNonBlank(start.stderr(), start.stdout()));
        }
        if (request.autostart()) {
            CommandResult autostart = libvirtCli.netAutostart(name, true);
            if (!autostart.succeeded()) {
                log.warn("Network {} started but autostart failed: {}", name, firstNonBlank(autostart.stderr(), autostart.stdout()));
            }
        }
        log.info("Network created successfully in libvirt: {}", name);
        return describe(name);
    }

    @Override
    public void startNetwork(String name) {
        requireConnected();
        String network = LibvirtNames.requireSafe(name, "network");
        requireExisting(network);
        log.info("Starting libvirt network {}", network);
        CommandResult result = libvirtCli.netStart(network);
        if (!result.succeeded() && !containsIgnoreCase(result.stderr(), "already active")) {
            throw new NetworkOperationException("Failed to start network: " + firstNonBlank(result.stderr(), result.stdout()));
        }
        log.info("Network started: {}", network);
    }

    @Override
    public void stopNetwork(String name) {
        requireConnected();
        String network = LibvirtNames.requireSafe(name, "network");
        requireExisting(network);
        log.info("Stopping libvirt network {}", network);
        CommandResult result = libvirtCli.netDestroy(network);
        if (!result.succeeded()
                && !containsIgnoreCase(result.stderr(), "not active")
                && !containsIgnoreCase(result.stderr(), "is not running")) {
            throw new NetworkOperationException("Failed to stop network: " + firstNonBlank(result.stderr(), result.stdout()));
        }
        log.info("Network stopped: {}", network);
    }

    @Override
    public void deleteNetwork(String name) {
        requireConnected();
        String network = LibvirtNames.requireSafe(name, "network");
        if ("default".equals(network)) {
            throw new NetworkInUseException("The libvirt default network is the host NAT. MiniCloud will not undefine it.");
        }
        VirtualNetworkInfo current = describe(network);
        if (current.active()) {
            throw new NetworkInUseException("libvirt will not undefine an active network. Stop '" + network + "' first.");
        }
        log.info("Undefining libvirt network {}", network);
        CommandResult result = libvirtCli.netUndefine(network);
        if (!result.succeeded()) {
            throw new NetworkOperationException("Failed to delete network: " + firstNonBlank(result.stderr(), result.stdout()));
        }
        log.info("Network deleted: {}", network);
    }

    @Override
    public void setAutostart(String name, boolean enabled) {
        requireConnected();
        String network = LibvirtNames.requireSafe(name, "network");
        requireExisting(network);
        log.info("Setting autostart={} for network {}", enabled, network);
        CommandResult result = libvirtCli.netAutostart(network, enabled);
        if (!result.succeeded()) {
            throw new NetworkOperationException("Failed to change autostart: " + firstNonBlank(result.stderr(), result.stdout()));
        }
    }

    private VirtualNetworkInfo describe(String name) {
        CommandResult info = libvirtCli.netInfo(name);
        if (!info.succeeded()) {
            if (LibvirtOutputParser.looksLikeMissingNetwork(new LibvirtOutputParser.CommandHint(info.stdout(), info.stderr()))) {
                throw new NetworkNotFoundException(name);
            }
            throw new NetworkNotFoundException(name);
        }
        String stdout = info.stdout();
        boolean active = yes(LibvirtOutputParser.parseKeyedField(stdout, "Active"));
        boolean persistent = yes(LibvirtOutputParser.parseKeyedField(stdout, "Persistent"));
        boolean autostart = yes(LibvirtOutputParser.parseKeyedField(stdout, "Autostart"));
        String uuid = LibvirtOutputParser.parseKeyedField(stdout, "UUID");
        String bridge = LibvirtOutputParser.parseKeyedField(stdout, "Bridge");
        String state = active ? "active" : "inactive";

        String forwardMode = null;
        String cidr = null;
        String gateway = null;
        String dhcpStart = null;
        String dhcpEnd = null;
        CommandResult xml = libvirtCli.netDumpXml(name);
        if (xml.succeeded()) {
            NetworkXmlParser.Addressing addressing = NetworkXmlParser.parse(xml.stdout());
            forwardMode = addressing.forwardMode();
            cidr = addressing.cidr();
            gateway = addressing.gateway();
            dhcpStart = addressing.dhcpStart();
            dhcpEnd = addressing.dhcpEnd();
            if (bridge == null) {
                bridge = addressing.bridge();
            }
            if (uuid == null) {
                uuid = addressing.uuid();
            }
        }
        return new VirtualNetworkInfo(
                name, uuid, state, active, persistent, autostart,
                bridge, forwardMode, cidr, gateway, dhcpStart, dhcpEnd, List.of()
        );
    }

    private boolean exists(String name) {
        return libvirtCli.netInfo(name).succeeded();
    }

    private void requireExisting(String name) {
        if (!exists(name)) {
            throw new NetworkNotFoundException(name);
        }
    }

    private boolean connected() {
        return hostMetricsProvider.collect().libvirt().connectionSucceeded();
    }

    private void requireConnected() {
        if (!connected()) {
            throw new InfrastructureConnectionException("libvirt is not connected to " + libvirtCli.uri());
        }
    }

    private static boolean yes(String value) {
        return value != null && value.toLowerCase(Locale.ROOT).startsWith("yes");
    }

    private static boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "unknown error";
    }
}

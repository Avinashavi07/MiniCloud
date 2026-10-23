package com.minicloud.infrastructure.ovs;

import com.minicloud.common.exception.InvalidOvsConfigurationException;
import com.minicloud.common.exception.OvsAlreadyExistsException;
import com.minicloud.common.exception.OvsInUseException;
import com.minicloud.common.exception.OvsNotAvailableException;
import com.minicloud.common.exception.OvsNotFoundException;
import com.minicloud.common.exception.OvsOperationException;
import com.minicloud.infrastructure.process.CommandResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Talks to Open vSwitch via ovs-vsctl. OVS is the source of truth.
 */
@Component
public class OpenVSwitchProvider implements OvsProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenVSwitchProvider.class);

    private final OvsCli ovsCli;

    public OpenVSwitchProvider(OvsCli ovsCli) {
        this.ovsCli = ovsCli;
    }

    @Override
    public OvsCapabilities capabilities() {
        CommandResult version = ovsCli.version();
        if (!version.succeeded()) {
            String stderr = firstNonBlank(version.stderr(), version.stdout());
            if (looksMissing(stderr)) {
                return new OvsCapabilities(false, false, "NOT_INSTALLED",
                        "ovs-vsctl is not installed. Install openvswitch-switch to enable Phase 7.",
                        null, null);
            }
            return new OvsCapabilities(true, false, "UNAVAILABLE",
                    "ovs-vsctl failed: " + stderr, null, null);
        }
        String ovsVersion = OvsOutputParser.parseVersion(version.stdout());
        String dbVersion = OvsOutputParser.parseDbVersion(version.stdout());
        CommandResult list = ovsCli.listBridges();
        if (!list.succeeded()) {
            String detail = firstNonBlank(list.stderr(), list.stdout());
            if (looksPermission(detail) || looksDatabaseDown(detail)) {
                return new OvsCapabilities(true, false, "PERMISSION_DENIED",
                        "OVS is installed but the database is unreachable: " + detail,
                        ovsVersion, dbVersion);
            }
            return new OvsCapabilities(true, false, "UNAVAILABLE", detail, ovsVersion, dbVersion);
        }
        return new OvsCapabilities(true, true, "CONNECTED",
                "Connected to Open vSwitch via ovs-vsctl.", ovsVersion, dbVersion);
    }

    @Override
    public List<OvsBridgeInfo> listBridges() {
        OvsCapabilities caps = capabilities();
        if (!caps.connected()) {
            log.info("Skipping OVS listing: status={}", caps.status());
            return List.of();
        }
        CommandResult result = ovsCli.listBridges();
        if (!result.succeeded()) {
            log.warn("ovs-vsctl list-br failed: {}", firstNonBlank(result.stderr(), result.stdout()));
            return List.of();
        }
        List<OvsBridgeInfo> bridges = new ArrayList<>();
        for (String name : OvsOutputParser.parseNameList(result.stdout())) {
            bridges.add(describe(name));
        }
        log.info("Open vSwitch reported {} bridge(s)", bridges.size());
        return bridges;
    }

    @Override
    public OvsBridgeInfo getBridge(String name) {
        requireConnected();
        OvsNames.requireSafe(name);
        if (!exists(name)) {
            throw new OvsNotFoundException(name);
        }
        return describe(name);
    }

    @Override
    public OvsBridgeInfo createBridge(CreateOvsBridgeRequest request) {
        requireConnected();
        String name = OvsNames.requireManagedCreateName(request.name());
        if (exists(name)) {
            throw new OvsAlreadyExistsException(name);
        }
        log.info("Creating OVS bridge {}", name);
        CommandResult result = ovsCli.addBridge(name);
        if (!result.succeeded()) {
            throw new OvsOperationException("ovs-vsctl add-br failed: " + firstNonBlank(result.stderr(), result.stdout()));
        }
        if (!exists(name)) {
            throw new OvsOperationException("ovs-vsctl reported success but bridge '" + name + "' is missing.");
        }
        return describe(name);
    }

    @Override
    public void deleteBridge(String name) {
        requireConnected();
        OvsNames.requireSafe(name);
        if (!exists(name)) {
            throw new OvsNotFoundException(name);
        }
        OvsBridgeInfo current = describe(name);
        if (!current.managedByMiniCloud()) {
            throw new InvalidOvsConfigurationException(
                    "Refusing to delete bridge '" + name + "'. MiniCloud only deletes bridges it created (mc-* with external_ids:minicloud=true).");
        }
        List<OvsPortInfo> foreign = current.ports().stream()
                .filter(port -> !port.name().equals(name))
                .toList();
        if (!foreign.isEmpty()) {
            throw new OvsInUseException(
                    "Bridge '" + name + "' still has ports " + foreign.stream().map(OvsPortInfo::name).toList()
                            + ". Detach VMs first.");
        }
        log.info("Deleting OVS bridge {}", name);
        CommandResult result = ovsCli.deleteBridge(name);
        if (!result.succeeded()) {
            throw new OvsOperationException("ovs-vsctl del-br failed: " + firstNonBlank(result.stderr(), result.stdout()));
        }
    }

    private OvsBridgeInfo describe(String name) {
        String uuid = OvsOutputParser.stripQuotes(succeededOrNull(ovsCli.getBridgeUuid(name)));
        String externalIds = succeededOrNull(ovsCli.getBridgeExternalIds(name));
        boolean managed = OvsOutputParser.externalIdsMarkManaged(externalIds) || OvsNames.looksManaged(name);
        String datapathId = OvsOutputParser.stripQuotes(succeededOrNull(ovsCli.getBridgeDatapathId(name)));
        List<OvsPortInfo> ports = portsOf(name);
        String detail = managed
                ? "MiniCloud-managed OVS bridge"
                : "Host OVS bridge (read-only in MiniCloud)";
        return new OvsBridgeInfo(name, uuid, managed, ports, datapathId, detail);
    }

    private List<OvsPortInfo> portsOf(String bridge) {
        CommandResult result = ovsCli.listPorts(bridge);
        if (!result.succeeded()) {
            return List.of();
        }
        List<OvsPortInfo> ports = new ArrayList<>();
        for (String port : OvsOutputParser.parseNameList(result.stdout())) {
            String uuid = OvsOutputParser.stripQuotes(succeededOrNull(ovsCli.getPortUuid(port)));
            String type = OvsOutputParser.stripQuotes(succeededOrNull(ovsCli.getInterfaceType(port)));
            if (type == null || type.isBlank()) {
                type = "system";
            }
            String ofport = OvsOutputParser.stripQuotes(succeededOrNull(ovsCli.getInterfaceOfport(port)));
            ports.add(new OvsPortInfo(port, uuid, type, List.of(port), ofport));
        }
        return ports;
    }

    private boolean exists(String name) {
        CommandResult result = ovsCli.brExists(name);
        return result.succeeded();
    }

    private void requireConnected() {
        OvsCapabilities caps = capabilities();
        if (!caps.connected()) {
            throw new OvsNotAvailableException(caps.detail());
        }
    }

    private String succeededOrNull(CommandResult result) {
        return result.succeeded() ? result.stdout() : null;
    }

    private static boolean looksMissing(String message) {
        if (message == null) {
            return true;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("no such file") || lower.contains("not found") || lower.contains("cannot run program");
    }

    private static boolean looksPermission(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("permission") || lower.contains("denied") || lower.contains("connect");
    }

    private static boolean looksDatabaseDown(String message) {
        if (message == null) {
            return false;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("database connection failed") || lower.contains("failed to connect");
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

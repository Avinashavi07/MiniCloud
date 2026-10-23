package com.minicloud.infrastructure.ovs;

import com.minicloud.infrastructure.process.CommandResult;
import com.minicloud.infrastructure.process.ControlledProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixed-argument ovs-vsctl wrapper. Untrusted names are validated first.
 */
@Component
public class OvsCli {

    private static final Logger log = LoggerFactory.getLogger(OvsCli.class);

    public CommandResult version() {
        return run(5, "--version");
    }

    public CommandResult listBridges() {
        return run(8, "list-br");
    }

    public CommandResult brExists(String bridge) {
        return run(5, "br-exists", OvsNames.requireSafe(bridge));
    }

    public CommandResult brGet(String bridge, String column) {
        return run(5, "br-get", OvsNames.requireSafe(bridge), column);
    }

    public CommandResult getBridgeUuid(String bridge) {
        return run(5, "get", "Bridge", OvsNames.requireSafe(bridge), "_uuid");
    }

    public CommandResult getBridgeExternalIds(String bridge) {
        return run(5, "get", "Bridge", OvsNames.requireSafe(bridge), "external_ids");
    }

    public CommandResult getBridgeDatapathId(String bridge) {
        return run(5, "get", "Bridge", OvsNames.requireSafe(bridge), "datapath_id");
    }

    public CommandResult listPorts(String bridge) {
        return run(8, "list-ports", OvsNames.requireSafe(bridge));
    }

    public CommandResult listIfaces(String bridge) {
        return run(8, "list-ifaces", OvsNames.requireSafe(bridge));
    }

    public CommandResult getPortUuid(String port) {
        return run(5, "get", "Port", OvsNames.requireSafe(port), "_uuid");
    }

    public CommandResult getPortType(String port) {
        return run(5, "get", "Port", OvsNames.requireSafe(port), "type");
    }

    public CommandResult getPortInterfaces(String port) {
        return run(5, "get", "Port", OvsNames.requireSafe(port), "interfaces");
    }

    public CommandResult getInterfaceOfport(String iface) {
        return run(5, "get", "Interface", OvsNames.requireSafe(iface), "ofport");
    }

    public CommandResult getInterfaceType(String iface) {
        return run(5, "get", "Interface", OvsNames.requireSafe(iface), "type");
    }

    public CommandResult addBridge(String bridge) {
        String name = OvsNames.requireManagedCreateName(bridge);
        return run(15,
                "--may-exist", "add-br", name,
                "--", "set", "Bridge", name, "external_ids:" + OvsNames.EXTERNAL_ID_KEY + "=" + OvsNames.EXTERNAL_ID_VALUE);
    }

    public CommandResult deleteBridge(String bridge) {
        return run(15, "--if-exists", "del-br", OvsNames.requireSafe(bridge));
    }

    public CommandResult show() {
        return run(8, "show");
    }

    private CommandResult run(long timeoutSeconds, String... ovsArgs) {
        List<String> command = new ArrayList<>();
        command.add("ovs-vsctl");
        command.add("--timeout=5");
        command.addAll(List.of(ovsArgs));
        log.debug("ovs command: {}", String.join(" ", command));
        return ControlledProcess.run(timeoutSeconds, command);
    }
}

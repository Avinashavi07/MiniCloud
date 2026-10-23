package com.minicloud.infrastructure.ceph;

import com.minicloud.infrastructure.process.CommandResult;
import com.minicloud.infrastructure.process.ControlledProcess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Fixed-argument ceph / rbd wrapper. Names are validated before append.
 * Prefers MicroCeph conf/keyring when present so the snap client can authenticate.
 */
@Component
public class CephCli {

    private static final Logger log = LoggerFactory.getLogger(CephCli.class);

    static final Path SNAP_CONF = Path.of("/var/snap/microceph/current/conf/ceph.conf");
    static final Path SNAP_KEYRING = Path.of("/var/snap/microceph/current/conf/ceph.client.admin.keyring");
    static final Path ETC_CONF = Path.of("/etc/ceph/ceph.conf");
    static final Path ETC_KEYRING = Path.of("/etc/ceph/ceph.client.admin.keyring");

    public CommandResult version() {
        return ceph(8, "--version");
    }

    public CommandResult status() {
        return ceph(15, "-s");
    }

    public CommandResult health() {
        return ceph(10, "health", "detail");
    }

    public CommandResult df() {
        return ceph(10, "df", "-f", "json");
    }

    public CommandResult osdStat() {
        return ceph(10, "osd", "stat", "-f", "json");
    }

    public CommandResult monStat() {
        return ceph(10, "mon", "stat", "-f", "json");
    }

    public CommandResult fsid() {
        return ceph(8, "fsid");
    }

    public CommandResult poolLs() {
        return ceph(10, "osd", "pool", "ls", "-f", "json");
    }

    public CommandResult poolLsDetail() {
        return ceph(15, "osd", "pool", "ls", "detail", "-f", "json");
    }

    public CommandResult poolCreate(String pool, int pgNum) {
        String name = CephNames.requireManagedPool(pool);
        int pgs = Math.max(1, Math.min(pgNum, 128));
        return ceph(60, "osd", "pool", "create", name, String.valueOf(pgs), String.valueOf(pgs));
    }

    public CommandResult poolSet(String pool, String key, String value) {
        return ceph(20, "osd", "pool", "set",
                CephNames.requireSafe(pool, "pool"), key, value);
    }

    public CommandResult poolApplicationEnable(String pool, String app) {
        return ceph(20, "osd", "pool", "application", "enable",
                CephNames.requireSafe(pool, "pool"), app);
    }

    public CommandResult poolDelete(String pool) {
        String name = CephNames.requireManagedPool(pool);
        return ceph(60, "osd", "pool", "delete", name, name, "--yes-i-really-really-mean-it");
    }

    public CommandResult rbdPoolInit(String pool) {
        return rbd(20, "pool", "init", CephNames.requireSafe(pool, "pool"));
    }

    public CommandResult rbdLs(String pool) {
        return rbd(15, "ls", "-p", CephNames.requireSafe(pool, "pool"), "--format", "json");
    }

    public CommandResult rbdInfo(String pool, String image) {
        return rbd(15, "info",
                CephNames.requireSafe(pool, "pool") + "/" + CephNames.requireImage(image),
                "--format", "json");
    }

    public CommandResult rbdCreate(String pool, String image, long sizeGb) {
        long gb = Math.max(1, Math.min(sizeGb, 2048));
        return rbd(60, "create",
                CephNames.requireSafe(pool, "pool") + "/" + CephNames.requireImage(image),
                "--size", gb + "G");
    }

    public CommandResult rbdRemove(String pool, String image) {
        return rbd(60, "rm",
                CephNames.requireSafe(pool, "pool") + "/" + CephNames.requireImage(image));
    }

    public CommandResult rbdDu(String pool, String image) {
        return rbd(20, "du",
                CephNames.requireSafe(pool, "pool") + "/" + CephNames.requireImage(image),
                "--format", "json");
    }

    private CommandResult ceph(long timeoutSeconds, String... args) {
        return run(timeoutSeconds, resolveCephBinary(), true, args);
    }

    private CommandResult rbd(long timeoutSeconds, String... args) {
        return run(timeoutSeconds, resolveRbdBinary(), true, args);
    }

    private CommandResult run(long timeoutSeconds, String binary, boolean withAuth, String... args) {
        List<String> command = new ArrayList<>();
        command.add(binary);
        if (withAuth && !isVersionOnly(args)) {
            Path conf = resolveConf();
            Path keyring = resolveKeyring();
            if (conf != null) {
                command.add("-c");
                command.add(conf.toString());
            }
            if (keyring != null) {
                command.add("-k");
                command.add(keyring.toString());
            }
        }
        command.addAll(List.of(args));
        log.debug("ceph command: {}", redact(command));
        return ControlledProcess.run(timeoutSeconds, command);
    }

    private static boolean isVersionOnly(String... args) {
        return args.length == 1 && "--version".equals(args[0]);
    }

    static Path resolveConf() {
        if (Files.isRegularFile(SNAP_CONF)) {
            return SNAP_CONF;
        }
        if (Files.isRegularFile(ETC_CONF)) {
            return ETC_CONF;
        }
        return null;
    }

    static Path resolveKeyring() {
        if (Files.isReadable(SNAP_KEYRING)) {
            return SNAP_KEYRING;
        }
        if (Files.isReadable(ETC_KEYRING)) {
            return ETC_KEYRING;
        }
        return null;
    }

    static String resolveCephBinary() {
        if (Files.isExecutable(Path.of("/snap/bin/ceph"))) {
            return "/snap/bin/ceph";
        }
        if (Files.isExecutable(Path.of("/usr/bin/ceph"))) {
            return "/usr/bin/ceph";
        }
        return "ceph";
    }

    static String resolveRbdBinary() {
        if (Files.isExecutable(Path.of("/snap/bin/rbd"))) {
            return "/snap/bin/rbd";
        }
        if (Files.isExecutable(Path.of("/usr/bin/rbd"))) {
            return "/usr/bin/rbd";
        }
        return "rbd";
    }

    private static String redact(List<String> command) {
        List<String> copy = new ArrayList<>(command.size());
        for (int i = 0; i < command.size(); i++) {
            String part = command.get(i);
            if ("-k".equals(part) && i + 1 < command.size()) {
                copy.add("-k");
                copy.add("<keyring>");
                i++;
                continue;
            }
            copy.add(part);
        }
        return String.join(" ", copy);
    }
}


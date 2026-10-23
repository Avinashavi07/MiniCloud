package com.minicloud.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@ConfigurationProperties(prefix = "minicloud")
public class MiniCloudProperties {

    private int phase = 1;
    private final Hypervisor hypervisor = new Hypervisor();
    private final Security security = new Security();
    private final Bootstrap bootstrap = new Bootstrap();

    public int getPhase() {
        return phase;
    }

    public void setPhase(int phase) {
        this.phase = phase;
    }

    public Hypervisor getHypervisor() {
        return hypervisor;
    }

    public Security getSecurity() {
        return security;
    }

    public Bootstrap getBootstrap() {
        return bootstrap;
    }

    public static class Hypervisor {
        private String type = "libvirt";
        private String uri = "qemu:///system";
        private String storagePool = "default";
        private String storagePoolPath = "/var/lib/libvirt/images";
        private String emulator = "/usr/bin/qemu-system-x86_64";
        private List<String> isoDirectories = List.of(
                "/var/lib/libvirt/images",
                System.getProperty("user.home") + "/Downloads"
        );

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getStoragePool() {
            return storagePool;
        }

        public void setStoragePool(String storagePool) {
            this.storagePool = storagePool;
        }

        public String getStoragePoolPath() {
            return storagePoolPath;
        }

        public void setStoragePoolPath(String storagePoolPath) {
            this.storagePoolPath = storagePoolPath;
        }

        public String getEmulator() {
            return emulator;
        }

        public void setEmulator(String emulator) {
            this.emulator = emulator;
        }

        public List<String> getIsoDirectories() {
            return isoDirectories;
        }

        public void setIsoDirectories(List<String> isoDirectories) {
            this.isoDirectories = isoDirectories;
        }
    }

    public static class Security {
        private String jwtSecret;
        private long jwtExpirationMs = 86_400_000L;
        private String corsAllowedOrigins = "http://localhost:5173";

        public String getJwtSecret() {
            return jwtSecret;
        }

        public void setJwtSecret(String jwtSecret) {
            this.jwtSecret = jwtSecret;
        }

        public long getJwtExpirationMs() {
            return jwtExpirationMs;
        }

        public void setJwtExpirationMs(long jwtExpirationMs) {
            this.jwtExpirationMs = jwtExpirationMs;
        }

        public String getCorsAllowedOrigins() {
            return corsAllowedOrigins;
        }

        public void setCorsAllowedOrigins(String corsAllowedOrigins) {
            this.corsAllowedOrigins = corsAllowedOrigins;
        }
    }

    public static class Bootstrap {
        private final BootstrapUser admin = new BootstrapUser();
        private final BootstrapUser operator = new BootstrapUser();
        private final BootstrapUser viewer = new BootstrapUser();

        public BootstrapUser getAdmin() {
            return admin;
        }

        public BootstrapUser getOperator() {
            return operator;
        }

        public BootstrapUser getViewer() {
            return viewer;
        }
    }

    public static class BootstrapUser {
        private String username;
        private String password;
        private String displayName;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }
    }
}

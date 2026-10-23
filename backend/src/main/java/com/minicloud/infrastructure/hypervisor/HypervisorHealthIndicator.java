package com.minicloud.infrastructure.hypervisor;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class HypervisorHealthIndicator implements HealthIndicator {

    private final HypervisorProvider hypervisorProvider;

    public HypervisorHealthIndicator(HypervisorProvider hypervisorProvider) {
        this.hypervisorProvider = hypervisorProvider;
    }

    @Override
    public Health health() {
        HypervisorCapabilities capabilities = hypervisorProvider.capabilities();
        Health.Builder builder = capabilities.connected() ? Health.up() : Health.unknown();
        return builder
                .withDetail("provider", capabilities.providerType())
                .withDetail("uri", capabilities.uri())
                .withDetail("kvmAvailable", capabilities.kvmAvailable())
                .withDetail("status", capabilities.status())
                .withDetail("detail", capabilities.detail())
                .build();
    }
}

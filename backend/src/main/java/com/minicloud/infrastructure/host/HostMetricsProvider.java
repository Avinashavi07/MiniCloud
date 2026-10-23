package com.minicloud.infrastructure.host;

/**
 * Reads live host inventory and resource usage from the operating system.
 * Implementations must not invent metrics when the OS cannot supply them.
 */
public interface HostMetricsProvider {

    HostSnapshot collect();
}

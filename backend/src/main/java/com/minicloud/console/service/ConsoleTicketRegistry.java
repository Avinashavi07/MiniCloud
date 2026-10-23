package com.minicloud.console.service;

import com.minicloud.infrastructure.console.ConsoleConnectionInfo;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Short-lived tickets for the localhost VNC WebSocket bridge.
 * The ticket is the WebSocket credential; VNC itself stays on 127.0.0.1.
 */
@Component
public class ConsoleTicketRegistry {

    public static final long TTL_MS = 5 * 60 * 1000L;

    private final SecureRandom random = new SecureRandom();
    private final Map<String, Ticket> tickets = new ConcurrentHashMap<>();

    public Ticket issue(String username, ConsoleConnectionInfo console) {
        purgeExpired();
        String id = HexFormat.of().formatHex(random.generateSeed(16));
        Ticket ticket = new Ticket(
                id,
                username,
                console.vmName(),
                console.host(),
                console.port(),
                Instant.now().plusMillis(TTL_MS).toEpochMilli()
        );
        tickets.put(id, ticket);
        return ticket;
    }

    public Ticket consumeValid(String id) {
        purgeExpired();
        if (id == null || id.isBlank()) {
            return null;
        }
        Ticket ticket = tickets.get(id);
        if (ticket == null || ticket.expired()) {
            tickets.remove(id);
            return null;
        }
        return ticket;
    }

    public void revoke(String id) {
        if (id != null) {
            tickets.remove(id);
        }
    }

    private void purgeExpired() {
        long now = Instant.now().toEpochMilli();
        tickets.entrySet().removeIf(entry -> entry.getValue().expiresAtEpochMs() <= now);
    }

    public record Ticket(String id, String username, String vmName, String host, int port, long expiresAtEpochMs) {
        public boolean expired() {
            return Instant.now().toEpochMilli() >= expiresAtEpochMs;
        }
    }
}

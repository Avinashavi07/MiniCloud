package com.minicloud.console.ws;

import com.minicloud.console.service.ConsoleService;
import com.minicloud.console.service.ConsoleTicketRegistry;
import com.minicloud.infrastructure.console.ConsoleConnectionInfo;
import com.minicloud.infrastructure.console.ConsoleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * websockify-style TCP bridge. MiniCloud does not decode RFB.
 * It copies bytes between the browser (noVNC) and QEMU VNC on 127.0.0.1.
 */
@Component
public class VncWebSocketHandler extends BinaryWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(VncWebSocketHandler.class);
    private static final int MAX_PORT = 5999;
    private static final int MIN_PORT = 5900;

    private final ConsoleTicketRegistry tickets;
    private final ConsoleProvider consoleProvider;
    private final ExecutorService pump = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r, "vnc-bridge");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, Bridge> bridges = new ConcurrentHashMap<>();

    public VncWebSocketHandler(ConsoleTicketRegistry tickets, ConsoleProvider consoleProvider) {
        this.tickets = tickets;
        this.consoleProvider = consoleProvider;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        session.setBinaryMessageSizeLimit(1024 * 1024);
        String ticketId = query(session, "ticket");
        ConsoleTicketRegistry.Ticket ticket = tickets.consumeValid(ticketId);
        if (ticket == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Invalid or expired console ticket"));
            return;
        }
        ConsoleConnectionInfo live = consoleProvider.getConsole(ticket.vmName());
        if (!live.available() || live.port() == null || !ConsoleService.isLoopback(live.host())
                || live.port() < MIN_PORT || live.port() > MAX_PORT) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("VNC is not available on loopback"));
            return;
        }
        if (live.port() != ticket.port()) {
            log.info("VNC port for {} changed from {} to {}", ticket.vmName(), ticket.port(), live.port());
        }
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(), live.port()), 3000);
        socket.setTcpNoDelay(true);
        Bridge bridge = new Bridge(session, socket);
        bridges.put(session.getId(), bridge);
        pump.execute(() -> pumpFromQemu(bridge));
        log.info("Bridged console session {} to {}:{}", ticket.vmName(), live.host(), live.port());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        Bridge bridge = bridges.get(session.getId());
        if (bridge == null) {
            return;
        }
        ByteBuffer payload = message.getPayload();
        byte[] bytes = new byte[payload.remaining()];
        payload.get(bytes);
        OutputStream out = bridge.socket.getOutputStream();
        out.write(bytes);
        out.flush();
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        close(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.debug("Console websocket error: {}", exception.toString());
        close(session.getId());
    }

    private void pumpFromQemu(Bridge bridge) {
        try (InputStream in = bridge.socket.getInputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                if (read == 0) {
                    continue;
                }
                if (!bridge.session.isOpen()) {
                    break;
                }
                byte[] chunk = new byte[read];
                System.arraycopy(buffer, 0, chunk, 0, read);
                synchronized (bridge.session) {
                    bridge.session.sendMessage(new BinaryMessage(chunk));
                }
            }
        } catch (IOException ex) {
            log.debug("VNC socket closed: {}", ex.toString());
        } finally {
            close(bridge.session.getId());
        }
    }

    private void close(String sessionId) {
        Bridge bridge = bridges.remove(sessionId);
        if (bridge == null) {
            return;
        }
        try {
            bridge.socket.close();
        } catch (IOException ignored) {
            // already closed
        }
        if (bridge.session.isOpen()) {
            try {
                bridge.session.close(CloseStatus.NORMAL);
            } catch (IOException ignored) {
                // already closed
            }
        }
    }

    private static String query(WebSocketSession session, String name) {
        if (session.getUri() == null) {
            return null;
        }
        return UriComponentsBuilder.fromUri(session.getUri()).build().getQueryParams().getFirst(name);
    }

    private record Bridge(WebSocketSession session, Socket socket) {
    }
}

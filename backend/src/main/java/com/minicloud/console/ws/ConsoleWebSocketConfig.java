package com.minicloud.console.ws;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class ConsoleWebSocketConfig implements WebSocketConfigurer {

    private final VncWebSocketHandler vncWebSocketHandler;

    public ConsoleWebSocketConfig(VncWebSocketHandler vncWebSocketHandler) {
        this.vncWebSocketHandler = vncWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(vncWebSocketHandler, "/ws/console")
                .setAllowedOriginPatterns("http://localhost:*", "http://127.0.0.1:*");
    }
}

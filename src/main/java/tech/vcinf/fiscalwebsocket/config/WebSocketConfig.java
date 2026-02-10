package tech.vcinf.fiscalwebsocket.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import tech.vcinf.fiscalwebsocket.controller.FiscalWebSocketHandler;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final FiscalWebSocketHandler fiscalWebSocketHandler;

    public WebSocketConfig(FiscalWebSocketHandler fiscalWebSocketHandler) {
        this.fiscalWebSocketHandler = fiscalWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(fiscalWebSocketHandler, "/ws").setAllowedOrigins("*");
    }
}

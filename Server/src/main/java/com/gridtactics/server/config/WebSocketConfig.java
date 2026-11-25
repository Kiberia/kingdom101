package com.gridtactics.server.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory broker; topics the client subscribes to
        registry.enableSimpleBroker("/topic");
        // Prefix for messages sent from client to server (@MessageMapping)
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Matches SockJS URL used in lobby.js: SERVER_URL + '/wsjs'
        registry.addEndpoint("/wsjs")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }
}

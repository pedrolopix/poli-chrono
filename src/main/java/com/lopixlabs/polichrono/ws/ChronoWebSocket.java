package com.lopixlabs.polichrono.ws;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lopixlabs.polichrono.service.SpeakerStore;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketConnection;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket(path = "/ws")
@ApplicationScoped
public class ChronoWebSocket {

    @Inject
    SpeakerStore store;

    @Inject
    ObjectMapper mapper;

    private final Set<WebSocketConnection> connections = ConcurrentHashMap.newKeySet();

    @OnOpen
    public void onOpen(WebSocketConnection connection) {
        connections.add(connection);
        // send initial state
        try {
            String payload = mapper.writeValueAsString(store.list());
            connection.sendTextAndAwait(payload);
        } catch (IOException e) {
            // ignore
        }
        // send initial autoStop flag
        try {
            String cfg = mapper.writeValueAsString(Map.of("type", "autoStop", "enabled", store.isAutoStopOnStart()));
            connection.sendTextAndAwait(cfg);
        } catch (Exception ignored) {
        }
    }

    @OnClose
    public void onClose(WebSocketConnection connection) {
        connections.remove(connection);
    }

    public void broadcastState() {
        String payload;
        try {
            payload = mapper.writeValueAsString(store.list());
        } catch (IOException e) {
            return;
        }
        for (WebSocketConnection c : connections) {
            try {
                c.sendTextAndAwait(payload);
            } catch (Exception ignored) {
            }
        }
    }

    public void broadcastAutoStop() {
        String cfg;
        try {
            cfg = mapper.writeValueAsString(Map.of("type", "autoStop", "enabled", store.isAutoStopOnStart()));
        } catch (Exception e) {
            return;
        }
        for (WebSocketConnection c : connections) {
            try {
                c.sendTextAndAwait(cfg);
            } catch (Exception ignored) {
            }
        }
    }
}

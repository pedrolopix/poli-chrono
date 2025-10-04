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
        // send initial title
        try {
            String t = mapper.writeValueAsString(Map.of("type", "title", "value", store.getTitle()));
            connection.sendTextAndAwait(t);
        } catch (Exception ignored) {
        }
        // send initial UI size settings (admin)
        try {
            String sz = mapper.writeValueAsString(Map.of(
                    "type", "size",
                    "cardWidth", store.getUiCardWidth(),
                    "textScale", store.getUiTextScale()
            ));
            connection.sendTextAndAwait(sz);
        } catch (Exception ignored) {
        }
        // send initial audience (main page) UI size settings
        try {
            String szm = mapper.writeValueAsString(Map.of(
                    "type", "sizeMain",
                    "cardWidth", store.getUiCardWidthMain(),
                    "textScale", store.getUiTextScaleMain()
            ));
            connection.sendTextAndAwait(szm);
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

    public void broadcastTitle() {
        String msg;
        try {
            msg = mapper.writeValueAsString(Map.of("type", "title", "value", store.getTitle()));
        } catch (Exception e) {
            return;
        }
        for (WebSocketConnection c : connections) {
            try {
                c.sendTextAndAwait(msg);
            } catch (Exception ignored) {
            }
        }
    }

    public void broadcastSize() {
        String msg;
        try {
            msg = mapper.writeValueAsString(Map.of(
                    "type", "size",
                    "cardWidth", store.getUiCardWidth(),
                    "textScale", store.getUiTextScale()
            ));
        } catch (Exception e) {
            return;
        }
        for (WebSocketConnection c : connections) {
            try {
                c.sendTextAndAwait(msg);
            } catch (Exception ignored) {
            }
        }
    }

    public void broadcastSizeMain() {
        String msg;
        try {
            msg = mapper.writeValueAsString(Map.of(
                    "type", "sizeMain",
                    "cardWidth", store.getUiCardWidthMain(),
                    "textScale", store.getUiTextScaleMain()
            ));
        } catch (Exception e) {
            return;
        }
        for (WebSocketConnection c : connections) {
            try {
                c.sendTextAndAwait(msg);
            } catch (Exception ignored) {
            }
        }
    }
}

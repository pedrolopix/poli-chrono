package com.lopixlabs.polichrono.ws;

import com.lopixlabs.polichrono.service.SpeakerStore;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class Ticker {

    @Inject
    SpeakerStore store;

    @Inject
    ChronoWebSocket ws;

    @Scheduled(every = "1s")
    void tick() {
        if (store.anyRunning()) {
            ws.broadcastState();
        }
    }
}

package com.lopixlabs.polichrono.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Speaker {
    private String id;
    private String name;
    private String faceUrl; // legacy: kept for backward compatibility (no longer used by UI)
    private String imageFilename; // stored on disk under data/images
    private long elapsedMillis; // accumulated time when not running
    private boolean running;
    @JsonIgnore
    private Long lastStartEpochMillis; // when started, to compute live elapsed

    public Speaker() {
        // default
    }

    public Speaker(String name, String faceUrl) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.faceUrl = faceUrl;
        this.elapsedMillis = 0L;
        this.running = false;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getFaceUrl() { return faceUrl; }
    public void setFaceUrl(String faceUrl) { this.faceUrl = faceUrl; }

    public String getImageFilename() { return imageFilename; }
    public void setImageFilename(String imageFilename) { this.imageFilename = imageFilename; }

    public long getElapsedMillis() {
        if (running && lastStartEpochMillis != null) {
            long now = Instant.now().toEpochMilli();
            return elapsedMillis + Math.max(0, now - lastStartEpochMillis);
        }
        return elapsedMillis;
    }

    public void setElapsedMillis(long elapsedMillis) { this.elapsedMillis = elapsedMillis; }

    public boolean isRunning() { return running; }
    public void setRunning(boolean running) { this.running = running; }

    @JsonIgnore
    public Long getLastStartEpochMillis() { return lastStartEpochMillis; }
    public void setLastStartEpochMillis(Long lastStartEpochMillis) { this.lastStartEpochMillis = lastStartEpochMillis; }

    public void start() {
        if (!running) {
            running = true;
            lastStartEpochMillis = Instant.now().toEpochMilli();
        }
    }

    public void stop() {
        if (running) {
            long now = Instant.now().toEpochMilli();
            if (lastStartEpochMillis != null) {
                elapsedMillis += Math.max(0, now - lastStartEpochMillis);
            }
            running = false;
            lastStartEpochMillis = null;
        }
    }

    public void reset() {
        running = false;
        elapsedMillis = 0;
        lastStartEpochMillis = null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Speaker speaker = (Speaker) o;
        return Objects.equals(id, speaker.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}

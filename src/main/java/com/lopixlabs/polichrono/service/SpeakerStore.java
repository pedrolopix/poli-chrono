package com.lopixlabs.polichrono.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lopixlabs.polichrono.model.Speaker;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@ApplicationScoped
public class SpeakerStore {

    @ConfigProperty(name = "speakers.file", defaultValue = "./data/speakers.json")
    String filePath;

    @Inject
    ObjectMapper mapper;

    @ConfigProperty(name = "images.dir", defaultValue = "./data/images")
    String imagesDir;

    @ConfigProperty(name = "chrono.autostop", defaultValue = "true")
    boolean defaultAutoStop;

    @ConfigProperty(name = "chrono.title", defaultValue = "")
    String defaultTitle;

    private volatile boolean autoStopOnStart;
    private volatile String title;
    // UI settings (not persisted to file): card width in px and text scale in %
    private volatile int uiCardWidth = 360;
    private volatile int uiTextScale = 100;
    // Admin action button size in px (Start/Stop); configurable via UI
    private volatile int uiActionSize = 56;
    // Audience (main page) UI settings: separate controls from admin
    private volatile int uiCardWidthMain = 360;
    private volatile int uiTextScaleMain = 100;

    private final List<Speaker> speakers = new CopyOnWriteArrayList<>();

    public List<Speaker> list() {
        // return a copy with computed elapsed
        List<Speaker> copy = new ArrayList<>();
        for (Speaker s : speakers) {
            Speaker c = new Speaker();
            c.setId(s.getId());
            c.setName(s.getName());
            c.setFaceUrl(s.getFaceUrl());
            c.setImageFilename(s.getImageFilename());
            c.setElapsedMillis(s.getElapsedMillis());
            c.setRunning(s.isRunning());
            copy.add(c);
        }
        return copy;
    }

    public Optional<Speaker> get(String id) {
        return speakers.stream().filter(s -> Objects.equals(s.getId(), id)).findFirst();
    }

    public Speaker create(String name, String faceUrl) {
        Speaker s = new Speaker(name, faceUrl);
        speakers.add(s);
        persist();
        return s;
    }

    public Speaker update(String id, String name, String faceUrl) {
        Speaker s = get(id).orElseThrow(NoSuchElementException::new);
        s.setName(name);
        s.setFaceUrl(faceUrl);
        persist();
        return s;
    }

    public void delete(String id) {
        get(id).ifPresent(s -> {
            s.stop();
            // delete image file if exists
            if (s.getImageFilename() != null && !s.getImageFilename().isBlank()) {
                try {
                    Path img = Path.of(imagesDir).resolve(s.getImageFilename());
                    Files.deleteIfExists(img);
                } catch (Exception ignored) {}
            }
            speakers.remove(s);
            persist();
        });
    }

    public synchronized void startOnly(String id) {
        // legacy: always stop all first, then start target
        long now = Instant.now().toEpochMilli();
        for (Speaker sp : speakers) {
            if (sp.isRunning()) {
                sp.stop();
            }
        }
        Speaker target = get(id).orElseThrow(NoSuchElementException::new);
        target.setLastStartEpochMillis(now);
        target.setRunning(true);
        persist();
    }

    public synchronized void start(String id) {
        long now = Instant.now().toEpochMilli();
        if (autoStopOnStart) {
            for (Speaker sp : speakers) {
                if (sp.isRunning() && !Objects.equals(sp.getId(), id)) {
                    sp.stop();
                }
            }
        }
        Speaker target = get(id).orElseThrow(NoSuchElementException::new);
        if (!target.isRunning()) {
            target.setLastStartEpochMillis(now);
            target.setRunning(true);
        } else if (!autoStopOnStart) {
            // if already running and autostop is off, keep running (noop)
        }
        persist();
    }

    public synchronized void stop(String id) {
        Speaker s = get(id).orElseThrow(NoSuchElementException::new);
        if (s.isRunning()) {
            s.stop();
            persist();
        }
    }

    public boolean isAutoStopOnStart() { return autoStopOnStart; }
    public void setAutoStopOnStart(boolean autoStopOnStart) { this.autoStopOnStart = autoStopOnStart; }

    public synchronized void stopAll() {
        boolean changed = false;
        for (Speaker sp : speakers) {
            if (sp.isRunning()) {
                sp.stop();
                changed = true;
            }
        }
        if (changed) persist();
    }

    public synchronized void resetAll() {
        boolean changed = false;
        for (Speaker sp : speakers) {
            long before = sp.getElapsedMillis();
            if (sp.isRunning() || before != 0L) {
                sp.reset();
                changed = true;
            }
        }
        if (changed) persist();
    }

    public synchronized void reorder(List<String> ids) {
        if (ids == null || ids.isEmpty()) return;
        Map<String, Speaker> byId = new LinkedHashMap<>();
        for (Speaker s : speakers) byId.put(s.getId(), s);
        List<Speaker> reordered = new ArrayList<>();
        for (String id : ids) {
            Speaker s = byId.remove(id);
            if (s != null) reordered.add(s);
        }
        // append any remaining speakers not present in ids preserving their current order
        reordered.addAll(byId.values());
        speakers.clear();
        speakers.addAll(reordered);
        persist();
    }

    public boolean anyRunning() {
        return speakers.stream().anyMatch(Speaker::isRunning);
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title == null ? "" : title; }

    public int getUiCardWidth() { return uiCardWidth; }
    public void setUiCardWidth(int uiCardWidth) { this.uiCardWidth = Math.max(200, Math.min(1000, uiCardWidth)); }

    public int getUiTextScale() { return uiTextScale; }
    public void setUiTextScale(int uiTextScale) { this.uiTextScale = Math.max(50, Math.min(200, uiTextScale)); }

    public int getUiActionSize() { return uiActionSize; }
    public void setUiActionSize(int uiActionSize) { this.uiActionSize = Math.max(32, Math.min(96, uiActionSize)); }

    public int getUiCardWidthMain() { return uiCardWidthMain; }
    public void setUiCardWidthMain(int uiCardWidthMain) { this.uiCardWidthMain = Math.max(200, Math.min(1000, uiCardWidthMain)); }

    public int getUiTextScaleMain() { return uiTextScaleMain; }
    public void setUiTextScaleMain(int uiTextScaleMain) { this.uiTextScaleMain = Math.max(50, Math.min(200, uiTextScaleMain)); }

    @PostConstruct
    void init() {
        autoStopOnStart = defaultAutoStop;
        title = defaultTitle;
        try {
            Path path = Path.of(filePath);
            if (!Files.exists(path)) {
                if (path.getParent() != null) Files.createDirectories(path.getParent());
                Files.writeString(path, "[]");
            }
            String json = Files.readString(path);
            List<Speaker> loaded = mapper.readValue(json, new TypeReference<List<Speaker>>(){ });
            // sanitize
            for (Speaker s : loaded) {
                s.setRunning(false);
                s.setLastStartEpochMillis(null);
            }
            speakers.clear();
            speakers.addAll(loaded);
        } catch (Exception e) {
            // start empty on error
            speakers.clear();
        }
    }

    public synchronized void persist() {
        try {
            Path path = Path.of(filePath);
            if (!Files.exists(path)) {
                if (path.getParent() != null) Files.createDirectories(path.getParent());
            }
            // save without volatile fields
            List<Map<String, Object>> simple = new ArrayList<>();
            for (Speaker s : speakers) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", s.getId());
                m.put("name", s.getName());
                m.put("faceUrl", s.getFaceUrl());
                m.put("imageFilename", s.getImageFilename());
                m.put("elapsedMillis", s.getElapsedMillis());
                m.put("running", false); // persisted as stopped
                simple.add(m);
            }
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(simple);
            Files.writeString(path, json);
        } catch (IOException e) {
            throw new RuntimeException("Failed to persist speakers", e);
        }
    }
}

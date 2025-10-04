package com.lopixlabs.polichrono.rest;

import com.lopixlabs.polichrono.model.Speaker;
import com.lopixlabs.polichrono.service.SpeakerStore;
import com.lopixlabs.polichrono.ws.ChronoWebSocket;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

@Path("/api/speakers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequestScoped
public class SpeakersResource {

    @Inject
    SpeakerStore store;

    @Inject
    ChronoWebSocket ws;

    @ConfigProperty(name = "images.dir", defaultValue = "./data/images")
    String imagesDir;

    @GET
    public List<Speaker> list() {
        return store.list();
    }

    @POST
    public Speaker create(Map<String, String> payload) {
        String name = payload.getOrDefault("name", "").trim();
        String faceUrl = payload.getOrDefault("faceUrl", "").trim();
        Speaker s = store.create(name, faceUrl);
        ws.broadcastState();
        return s;
    }

    @PUT
    @Path("/{id}")
    public Speaker update(@PathParam("id") String id, Map<String, String> payload) {
        String name = payload.getOrDefault("name", "").trim();
        String faceUrl = payload.getOrDefault("faceUrl", "").trim();
        Speaker s = store.update(id, name, faceUrl);
        ws.broadcastState();
        return s;
    }

    @DELETE
    @Path("/{id}")
    public Response delete(@PathParam("id") String id) {
        store.delete(id);
        ws.broadcastState();
        return Response.noContent().build();
    }

    @POST
    @Path("/{id}/start")
    public Response start(@PathParam("id") String id) {
        store.start(id);
        ws.broadcastState();
        return Response.ok().build();
    }

    @POST
    @Path("/{id}/stop")
    public Response stop(@PathParam("id") String id) {
        store.stop(id);
        ws.broadcastState();
        return Response.ok().build();
    }

    @POST
    @Path("/stopAll")
    public Response stopAll() {
        store.stopAll();
        ws.broadcastState();
        return Response.ok().build();
    }

    @GET
    @Path("/autoStop")
    public Map<String, Object> getAutoStop() {
        return Map.of("enabled", store.isAutoStopOnStart());
    }

    @POST
    @Path("/autoStop")
    public Map<String, Object> setAutoStop(Map<String, Object> payload) {
        Object v = payload.get("enabled");
        boolean enabled = false;
        if (v instanceof Boolean b) enabled = b;
        else if (v != null) enabled = Boolean.parseBoolean(String.valueOf(v));
        store.setAutoStopOnStart(enabled);
        ws.broadcastAutoStop();
        return Map.of("enabled", store.isAutoStopOnStart());
    }

    @GET
    @Path("/title")
    public Map<String, Object> getTitle() {
        return Map.of("title", store.getTitle());
    }

    @POST
    @Path("/title")
    public Map<String, Object> setTitle(Map<String, Object> payload) {
        Object v = payload.get("title");
        String title = v == null ? "" : String.valueOf(v);
        store.setTitle(title);
        ws.broadcastTitle();
        return Map.of("title", store.getTitle());
    }

    @POST
    @Path("/reorder")
    public Response reorder(List<String> ids) {
        store.reorder(ids);
        ws.broadcastState();
        return Response.ok().build();
    }

    @GET
    @Path("/size")
    public Map<String, Object> getSize() {
        return Map.of(
                "cardWidth", store.getUiCardWidth(),
                "textScale", store.getUiTextScale()
        );
    }

    @POST
    @Path("/size")
    public Map<String, Object> setSize(Map<String, Object> payload) {
        Object cw = payload.get("cardWidth");
        Object ts = payload.get("textScale");
        if (cw != null) {
            try { store.setUiCardWidth(Integer.parseInt(String.valueOf(cw))); } catch (Exception ignored) {}
        }
        if (ts != null) {
            try { store.setUiTextScale(Integer.parseInt(String.valueOf(ts))); } catch (Exception ignored) {}
        }
        ws.broadcastSize();
        return Map.of(
                "cardWidth", store.getUiCardWidth(),
                "textScale", store.getUiTextScale()
        );
    }

    @GET
    @Path("/sizeMain")
    public Map<String, Object> getSizeMain() {
        return Map.of(
                "cardWidth", store.getUiCardWidthMain(),
                "textScale", store.getUiTextScaleMain()
        );
    }

    @POST
    @Path("/sizeMain")
    public Map<String, Object> setSizeMain(Map<String, Object> payload) {
        Object cw = payload.get("cardWidth");
        Object ts = payload.get("textScale");
        if (cw != null) {
            try { store.setUiCardWidthMain(Integer.parseInt(String.valueOf(cw))); } catch (Exception ignored) {}
        }
        if (ts != null) {
            try { store.setUiTextScaleMain(Integer.parseInt(String.valueOf(ts))); } catch (Exception ignored) {}
        }
        ws.broadcastSizeMain();
        return Map.of(
                "cardWidth", store.getUiCardWidthMain(),
                "textScale", store.getUiTextScaleMain()
        );
    }

    @POST
    @Path("/reloadMain")
    public Response reloadMain() {
        ws.broadcastReloadMain();
        return Response.ok().build();
    }

    // Upload a speaker image as raw bytes (Content-Type: image/*)
    @POST
    @Path("/{id}/image")
    @Consumes({"image/png","image/jpeg","image/gif","image/webp","application/octet-stream"})
    @Produces(MediaType.TEXT_PLAIN)
    public Response uploadImage(@PathParam("id") String id,
                                @HeaderParam("Content-Type") String contentType,
                                InputStream body) {
        Speaker sp = store.get(id).orElseThrow(NotFoundException::new);
        String ext = switch (contentType == null ? "" : contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".bin";
        };
        try {
            java.nio.file.Path dir = java.nio.file.Path.of(imagesDir);
            if (!Files.exists(dir)) {
                if (dir.getParent() != null) Files.createDirectories(dir);
                Files.createDirectories(dir);
            }
            // ensure unique but stable per speaker: use speaker id as basename
            String filename = id + ext;
            java.nio.file.Path target = dir.resolve(filename);
            // write stream with size limit (5MB)
            long max = 5L * 1024 * 1024;
            long written = 0;
            try (var out = Files.newOutputStream(target)) {
                byte[] buf = new byte[8192];
                int r;
                while ((r = body.read(buf)) != -1) {
                    written += r;
                    if (written > max) {
                        try { out.flush(); } catch (Exception ignored) {}
                        try { Files.deleteIfExists(target); } catch (Exception ignored) {}
                        return Response.status(413).entity("Image too large (max 5MB)").build();
                    }
                    out.write(buf, 0, r);
                }
            }
            // delete old image if different filename
            if (sp.getImageFilename() != null && !sp.getImageFilename().equals(filename)) {
                try { Files.deleteIfExists(dir.resolve(sp.getImageFilename())); } catch (Exception ignored) {}
            }
            sp.setImageFilename(filename);
            store.persist();
            ws.broadcastState();
            return Response.ok(filename).build();
        } catch (IOException e) {
            throw new InternalServerErrorException("Failed to save image");
        }
    }

    // Serve the stored image
    @GET
    @Path("/{id}/image")
    public Response getImage(@PathParam("id") String id) {
        Speaker sp = store.get(id).orElseThrow(NotFoundException::new);
        String filename = sp.getImageFilename();
        if (filename == null || filename.isBlank()) return Response.status(404).build();
        try {
            java.nio.file.Path file = java.nio.file.Path.of(imagesDir).resolve(filename);
            if (!Files.exists(file)) return Response.status(404).build();
            String ct;
            if (filename.endsWith(".png")) ct = "image/png";
            else if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) ct = "image/jpeg";
            else if (filename.endsWith(".gif")) ct = "image/gif";
            else if (filename.endsWith(".webp")) ct = "image/webp";
            else ct = Files.probeContentType(file);
            if (ct == null || ct.isBlank()) ct = "application/octet-stream";
            return Response.ok(Files.newInputStream(file)).type(ct).build();
        } catch (IOException e) {
            return Response.serverError().build();
        }
    }
}

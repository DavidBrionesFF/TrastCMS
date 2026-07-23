package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.config.TrastCmsProperties;
import com.nattechnologies.trastcms.domain.media.MediaAsset;
import com.nattechnologies.trastcms.domain.media.MediaAssetRepository;
import com.nattechnologies.trastcms.domain.media.MediaKind;
import com.nattechnologies.trastcms.plugin.api.PluginEvent;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MediaService {
    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif",
            "audio/mpeg",
            "audio/mp4",
            "audio/wav",
            "audio/x-wav",
            "audio/ogg",
            "video/mp4",
            "video/webm",
            "video/ogg",
            "application/pdf");

    private final MediaAssetRepository repository;
    private final Path uploadDirectory;
    private final AuditService audit;
    private final PluginEventDispatcher events;
    private final TrastCmsProperties.Media properties;

    public MediaService(
            MediaAssetRepository repository,
            TrastCmsProperties properties,
            AuditService audit,
            PluginEventDispatcher events) {
        this.repository = repository;
        this.uploadDirectory = Paths.get(
                        properties.getDataDir(), "uploads")
                .toAbsolutePath()
                .normalize();
        this.audit = audit;
        this.events = events;
        this.properties = properties.getMedia();

        try {
            Files.createDirectories(uploadDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo crear el directorio de medios", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.MediaResponse> list(
            String search, MediaKind kind, String folder) {
        String normalizedSearch = search == null
                ? ""
                : search.trim().toLowerCase(Locale.ROOT);
        return repository.findTop500ByOrderByCreatedAtDesc()
                .stream()
                .filter(asset -> kind == null || asset.getKind() == kind)
                .filter(asset -> folder == null || folder.isBlank()
                        || asset.getFolder().equalsIgnoreCase(folder.trim()))
                .filter(asset -> normalizedSearch.isBlank()
                        || contains(asset.getOriginalFilename(), normalizedSearch)
                        || contains(asset.getTitle(), normalizedSearch)
                        || contains(asset.getAltText(), normalizedSearch))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<ApiDtos.MediaResponse> uploadBatch(
            List<MultipartFile> files, String folder, String actor) {
        if (files == null || files.isEmpty()) {
            throw new BadRequestException("Debe seleccionar al menos un archivo");
        }
        if (files.size() > properties.getMaxBatchFiles()) {
            throw new BadRequestException(
                    "El lote supera el máximo de "
                            + properties.getMaxBatchFiles()
                            + " archivos");
        }
        return files.stream()
                .map(file -> upload(file, folder, actor))
                .collect(Collectors.toList());
    }

    @Transactional
    public ApiDtos.MediaResponse upload(
            MultipartFile file, String folder, String actor) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Debe seleccionar un archivo");
        }
        if (file.getSize() > properties.getMaxBytes()) {
            throw new BadRequestException(
                    "El archivo supera el límite configurado");
        }

        String contentType = Optional.ofNullable(file.getContentType())
                .orElse("application/octet-stream")
                .toLowerCase(Locale.ROOT);

        if (!ALLOWED_TYPES.contains(contentType)) {
            throw new BadRequestException(
                    "Tipo de archivo no permitido: " + contentType);
        }

        String original = Optional.ofNullable(file.getOriginalFilename())
                .orElse("archivo");
        original = Paths.get(original)
                .getFileName()
                .toString()
                .replaceAll("[^a-zA-Z0-9._-]", "_");

        String extension = original.contains(".")
                ? original.substring(original.lastIndexOf('.'))
                    .toLowerCase(Locale.ROOT)
                : "";
        String storedName = UUID.randomUUID() + extension;
        Path target = uploadDirectory.resolve(storedName).normalize();

        if (!target.startsWith(uploadDirectory)) {
            throw new BadRequestException("Ruta de archivo inválida");
        }

        try {
            file.transferTo(target);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo guardar el archivo", exception);
        }

        MediaAsset asset = new MediaAsset();
        asset.setFilename(storedName);
        asset.setOriginalFilename(original);
        asset.setTitle(stripExtension(original));
        asset.setContentType(contentType);
        asset.setKind(MediaKind.fromContentType(contentType));
        asset.setFolder(folder == null || folder.isBlank()
                ? "General"
                : folder.trim());
        asset.setSizeBytes(file.getSize());
        asset.setStoragePath(target.toString());
        asset.setPublicUrl("/api/public/media/pending");
        asset.setUploadedBy(actor);
        detectImageDimensions(asset, target);

        MediaAsset saved = repository.save(asset);
        saved.setPublicUrl("/api/public/media/" + saved.getId());
        saved = repository.save(saved);

        audit.record(
                actor,
                "media.uploaded",
                "media",
                saved.getId(),
                original);
        events.publish(PluginEvent.of("media.uploaded", Map.of(
                "id", saved.getId(),
                "contentType", contentType,
                "kind", saved.getKind().name(),
                "url", saved.getPublicUrl())));
        return toResponse(saved);
    }

    @Transactional
    public ApiDtos.MediaResponse update(
            String id, ApiDtos.MediaUpdateRequest request, String actor) {
        MediaAsset asset = requireAsset(id);
        asset.setTitle(blankToDefault(request.title(), asset.getTitle()));
        asset.setAltText(blankToNull(request.altText()));
        asset.setCaption(blankToNull(request.caption()));
        asset.setDescription(blankToNull(request.description()));
        asset.setFolder(blankToDefault(request.folder(), "General"));
        asset.setWidth(request.width());
        asset.setHeight(request.height());
        asset.setDurationSeconds(request.durationSeconds());

        MediaAsset saved = repository.save(asset);
        audit.record(
                actor,
                "media.updated",
                "media",
                id,
                saved.getOriginalFilename());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public MediaFile load(String id) {
        MediaAsset asset = requireAsset(id);
        Path path = Paths.get(asset.getStoragePath())
                .toAbsolutePath()
                .normalize();

        if (!path.startsWith(uploadDirectory)) {
            throw new BadRequestException("Ruta de medio inválida");
        }

        Resource resource = new FileSystemResource(path);
        if (!resource.exists() || !resource.isReadable()) {
            throw new NotFoundException("El archivo físico no existe");
        }
        return new MediaFile(
                resource,
                asset.getContentType(),
                asset.getOriginalFilename(),
                asset.getKind());
    }

    @Transactional
    public void delete(String id, String actor) {
        MediaAsset asset = requireAsset(id);
        Path path = Paths.get(asset.getStoragePath())
                .toAbsolutePath()
                .normalize();

        if (!path.startsWith(uploadDirectory)) {
            throw new BadRequestException("Ruta de medio inválida");
        }

        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "No se pudo eliminar el archivo físico", exception);
        }

        repository.delete(asset);
        audit.record(
                actor,
                "media.deleted",
                "media",
                id,
                asset.getOriginalFilename());
    }

    private MediaAsset requireAsset(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Archivo no encontrado"));
    }

    private void detectImageDimensions(MediaAsset asset, Path target) {
        if (asset.getKind() != MediaKind.IMAGE) return;
        try {
            BufferedImage image = ImageIO.read(target.toFile());
            if (image != null) {
                asset.setWidth(image.getWidth());
                asset.setHeight(image.getHeight());
            }
        } catch (IOException ignored) {
            // Las dimensiones son metadatos opcionales.
        }
    }

    private ApiDtos.MediaResponse toResponse(MediaAsset asset) {
        return new ApiDtos.MediaResponse(
                asset.getId(),
                asset.getFilename(),
                asset.getOriginalFilename(),
                asset.getTitle(),
                asset.getAltText(),
                asset.getCaption(),
                asset.getDescription(),
                asset.getKind(),
                asset.getFolder(),
                asset.getContentType(),
                asset.getSizeBytes(),
                asset.getWidth(),
                asset.getHeight(),
                asset.getDurationSeconds(),
                asset.getPublicUrl(),
                asset.getUploadedBy(),
                asset.getCreatedAt(),
                asset.getUpdatedAt());
    }

    private boolean contains(String value, String search) {
        return value != null
                && value.toLowerCase(Locale.ROOT).contains(search);
    }

    private String stripExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        return dot > 0 ? filename.substring(0, dot) : filename;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }

    public record MediaFile(
            Resource resource,
            String contentType,
            String originalFilename,
            MediaKind kind) {}
}

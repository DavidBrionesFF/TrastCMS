package com.nattechnologies.trastcms.web.admin;

import com.nattechnologies.trastcms.domain.media.MediaKind;
import com.nattechnologies.trastcms.service.MediaService;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/admin/media")
public class AdminMediaController {
    private final MediaService service;

    public AdminMediaController(MediaService service) {
        this.service = service;
    }

    @GetMapping
    public List<ApiDtos.MediaResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) MediaKind kind,
            @RequestParam(required = false) String folder) {
        return service.list(search, kind, folder);
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.MediaResponse upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String folder,
            Authentication auth) {
        return service.upload(file, folder, auth.getName());
    }

    @PostMapping(value = "/batch", consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public List<ApiDtos.MediaResponse> uploadBatch(
            @RequestPart("files") List<MultipartFile> files,
            @RequestParam(required = false) String folder,
            Authentication auth) {
        return service.uploadBatch(files, folder, auth.getName());
    }

    @PutMapping("/{id}")
    public ApiDtos.MediaResponse update(
            @PathVariable String id,
            @Valid @RequestBody ApiDtos.MediaUpdateRequest request,
            Authentication auth) {
        return service.update(id, request, auth.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable String id,
            Authentication auth) {
        service.delete(id, auth.getName());
    }
}

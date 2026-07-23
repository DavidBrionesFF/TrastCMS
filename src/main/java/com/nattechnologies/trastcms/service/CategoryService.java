package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.domain.category.*;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class CategoryService {
    private final CategoryRepository repository;
    private final SlugService slugService;
    private final AuditService audit;

    public CategoryService(CategoryRepository repository, SlugService slugService, AuditService audit) {
        this.repository = repository;
        this.slugService = slugService;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.CategoryResponse> list() {
        return repository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ApiDtos.CategoryResponse create(ApiDtos.CategoryRequest request, String actor) {
        String slug = slugService.slugify(request.slug() == null || request.slug().isBlank() ? request.name() : request.slug());
        if (repository.existsBySlug(slug)) throw new ConflictException("Ya existe una categoría con ese slug");
        if (repository.existsByNameIgnoreCase(request.name())) throw new ConflictException("Ya existe una categoría con ese nombre");
        Category category = new Category();
        apply(category, request, slug);
        category = repository.save(category);
        audit.record(actor, "category.created", "category", category.getId(), category.getName());
        return toResponse(category);
    }

    @Transactional
    public ApiDtos.CategoryResponse update(String id, ApiDtos.CategoryRequest request, String actor) {
        Category category = repository.findById(id).orElseThrow(() -> new NotFoundException("Categoría no encontrada"));
        String slug = slugService.slugify(request.slug() == null || request.slug().isBlank() ? request.name() : request.slug());
        if (repository.existsBySlugAndIdNot(slug, id)) throw new ConflictException("Ya existe una categoría con ese slug");
        if (repository.existsByNameIgnoreCaseAndIdNot(request.name(), id)) throw new ConflictException("Ya existe una categoría con ese nombre");
        apply(category, request, slug);
        audit.record(actor, "category.updated", "category", id, request.name());
        return toResponse(repository.save(category));
    }

    @Transactional
    public void delete(String id, String actor) {
        Category category = repository.findById(id).orElseThrow(() -> new NotFoundException("Categoría no encontrada"));
        try {
            repository.delete(category);
            repository.flush();
        } catch (RuntimeException exception) {
            throw new ConflictException("La categoría está siendo utilizada por contenido y no puede eliminarse");
        }
        audit.record(actor, "category.deleted", "category", id, category.getName());
    }

    private void apply(Category category, ApiDtos.CategoryRequest request, String slug) {
        category.setName(request.name().trim());
        category.setSlug(slug);
        category.setDescription(request.description() == null ? null : request.description().trim());
    }

    public ApiDtos.CategoryResponse toResponse(Category c) {
        return new ApiDtos.CategoryResponse(c.getId(), c.getName(), c.getSlug(), c.getDescription(), c.getCreatedAt(), c.getUpdatedAt());
    }
}

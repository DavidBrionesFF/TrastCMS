package com.nattechnologies.trastcms.web.admin;

import com.nattechnologies.trastcms.service.CategoryService;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {
    private final CategoryService service;
    public AdminCategoryController(CategoryService service) { this.service = service; }

    @GetMapping public List<ApiDtos.CategoryResponse> list() { return service.list(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.CategoryResponse create(@Valid @RequestBody ApiDtos.CategoryRequest request, Authentication auth) {
        return service.create(request, auth.getName());
    }

    @PutMapping("/{id}")
    public ApiDtos.CategoryResponse update(@PathVariable String id, @Valid @RequestBody ApiDtos.CategoryRequest request,
                                           Authentication auth) {
        return service.update(id, request, auth.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id, Authentication auth) { service.delete(id, auth.getName()); }
}

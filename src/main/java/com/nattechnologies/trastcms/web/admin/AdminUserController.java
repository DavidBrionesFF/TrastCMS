package com.nattechnologies.trastcms.web.admin;

import com.nattechnologies.trastcms.service.AdminUserService;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {
    private final AdminUserService service;

    public AdminUserController(AdminUserService service) { this.service = service; }

    @GetMapping
    public List<ApiDtos.UserResponse> list() { return service.list(); }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiDtos.UserResponse create(@Valid @RequestBody ApiDtos.UserRequest request, Authentication auth) {
        return service.create(request, auth.getName());
    }

    @PutMapping("/{id}")
    public ApiDtos.UserResponse update(@PathVariable String id, @Valid @RequestBody ApiDtos.UserRequest request,
                                       Authentication auth) {
        return service.update(id, request, auth.getName());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id, Authentication auth) {
        service.delete(id, auth.getName());
    }
}

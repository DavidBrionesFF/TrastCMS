package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.domain.post.PostRepository;
import com.nattechnologies.trastcms.domain.user.*;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminUserService {
    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;
    private final PostRepository posts;

    public AdminUserService(
            UserAccountRepository repository,
            PasswordEncoder passwordEncoder,
            AuditService audit,
            PostRepository posts) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
        this.posts = posts;
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.UserResponse> list() {
        return repository.findAllByOrderByDisplayNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public ApiDtos.UserResponse create(ApiDtos.UserRequest request, String actor) {
        String email = normalizeEmail(request.email());
        if (repository.existsByEmailIgnoreCase(email)) throw new ConflictException("Ya existe un usuario con ese correo");
        if (request.password() == null || request.password().isBlank()) {
            throw new BadRequestException("La contraseña es obligatoria para un usuario nuevo");
        }
        UserAccount account = new UserAccount();
        apply(account, request, email, true);
        account = repository.save(account);
        audit.record(actor, "user.created", "user", account.getId(), account.getEmail());
        return toResponse(account);
    }

    @Transactional
    public ApiDtos.UserResponse update(String id, ApiDtos.UserRequest request, String actor) {
        UserAccount account = require(id);
        String email = normalizeEmail(request.email());
        if (repository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw new ConflictException("Ya existe un usuario con ese correo");
        }
        boolean self = account.getEmail().equalsIgnoreCase(actor);
        if (self && !account.getEmail().equalsIgnoreCase(email)) {
            throw new BadRequestException("No puede cambiar el correo de su sesión actual");
        }
        if (self && (!request.enabled() || request.role() != UserRole.ADMIN)) {
            throw new BadRequestException("No puede desactivar ni retirar su propio rol de administrador");
        }
        protectLastAdministrator(account, request.role(), request.enabled());
        apply(account, request, email, false);
        account = repository.save(account);
        audit.record(actor, "user.updated", "user", account.getId(), account.getEmail());
        return toResponse(account);
    }

    @Transactional
    public void delete(String id, String actor) {
        UserAccount account = require(id);
        if (account.getEmail().equalsIgnoreCase(actor)) {
            throw new BadRequestException("No puede eliminar su propia cuenta");
        }
        if (account.getRole() == UserRole.ADMIN && account.isEnabled()
                && repository.countByRoleAndEnabledTrue(UserRole.ADMIN) <= 1) {
            throw new BadRequestException("Debe existir al menos un administrador activo");
        }
        repository.delete(account);
        audit.record(actor, "user.deleted", "user", id, account.getEmail());
    }

    private void apply(UserAccount account, ApiDtos.UserRequest request, String email, boolean creating) {
        account.setEmail(email);
        account.setDisplayName(request.displayName().trim());
        account.setRole(request.role());
        account.setEnabled(request.enabled());
        if (creating || (request.password() != null && !request.password().isBlank())) {
            account.setPasswordHash(passwordEncoder.encode(request.password()));
        }
    }

    private void protectLastAdministrator(UserAccount current, UserRole newRole, boolean enabled) {
        boolean removesActiveAdmin = current.getRole() == UserRole.ADMIN && current.isEnabled()
                && (newRole != UserRole.ADMIN || !enabled);
        if (removesActiveAdmin && repository.countByRoleAndEnabledTrue(UserRole.ADMIN) <= 1) {
            throw new BadRequestException("Debe existir al menos un administrador activo");
        }
    }

    private UserAccount require(String id) {
        return repository.findById(id).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    private String normalizeEmail(String value) {
        return value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private ApiDtos.UserResponse toResponse(UserAccount account) {
        return new ApiDtos.UserResponse(
                account.getId(),
                account.getEmail(),
                account.getDisplayName(),
                account.getRole(),
                account.isEnabled(),
                account.getAvatarUrl(),
                account.getLastLoginAt(),
                posts.countByAuthorEmailIgnoreCase(account.getEmail()),
                account.getCreatedAt(),
                account.getUpdatedAt());
    }
}

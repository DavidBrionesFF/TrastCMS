package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.domain.post.PostRepository;
import com.nattechnologies.trastcms.domain.site.*;
import com.nattechnologies.trastcms.domain.user.*;
import com.nattechnologies.trastcms.multisite.SiteContext;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class AdminUserService {
    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;
    private final PostRepository posts;
    private final CmsSiteRepository sites;
    private final CmsSiteMembershipRepository memberships;

    public AdminUserService(UserAccountRepository repository, PasswordEncoder passwordEncoder, AuditService audit,
                            PostRepository posts, CmsSiteRepository sites, CmsSiteMembershipRepository memberships) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
        this.posts = posts;
        this.sites = sites;
        this.memberships = memberships;
    }

    @Transactional(readOnly = true)
    public List<ApiDtos.UserResponse> list() {
        return memberships.findAllBySiteIdOrderByUserDisplayNameAsc(SiteContext.siteId()).stream()
                .map(this::toResponse).toList();
    }

    @Transactional
    public ApiDtos.UserResponse create(ApiDtos.UserRequest request, String actor) {
        String email = normalizeEmail(request.email());
        UserAccount account = repository.findByEmailIgnoreCase(email).orElse(null);
        if (account == null) {
            if (request.password() == null || request.password().isBlank()) throw new BadRequestException("La contraseña es obligatoria para un usuario nuevo");
            account = new UserAccount();
            account.setEmail(email);
            account.setDisplayName(request.displayName().trim());
            account.setRole(request.role());
            account.setEnabled(true);
            account.setNetworkAdmin(false);
            account.setPasswordHash(passwordEncoder.encode(request.password()));
            account = repository.save(account);
        } else if (memberships.findBySiteIdAndUserId(SiteContext.siteId(), account.getId()).isPresent()) {
            throw new ConflictException("Ese usuario ya pertenece a este sitio");
        }

        CmsSiteMembership membership = new CmsSiteMembership();
        membership.setSite(sites.findById(SiteContext.siteId()).orElseThrow(() -> new NotFoundException("Sitio no encontrado")));
        membership.setUser(account);
        membership.setRole(request.role());
        membership.setEnabled(request.enabled());
        membership = memberships.save(membership);
        promoteGlobalRole(account);
        audit.record(actor, "site.user.created", "user", account.getId(), SiteContext.siteKey() + ":" + email);
        return toResponse(membership);
    }

    @Transactional
    public ApiDtos.UserResponse update(String id, ApiDtos.UserRequest request, String actor) {
        UserAccount account = require(id);
        CmsSiteMembership membership = memberships.findBySiteIdAndUserId(SiteContext.siteId(), id)
                .orElseThrow(() -> new NotFoundException("El usuario no pertenece a este sitio"));
        boolean self = account.getEmail().equalsIgnoreCase(actor);
        if (self && (!request.enabled() || request.role() != UserRole.ADMIN)) {
            throw new BadRequestException("No puede desactivar ni retirar su propio rol de administrador en este sitio");
        }
        protectLastAdministrator(membership, request.role(), request.enabled());

        String email = normalizeEmail(request.email());
        if (!account.getEmail().equalsIgnoreCase(email)) {
            if (memberships.countByUserId(account.getId()) > 1) throw new BadRequestException("No puede cambiar el correo de una cuenta compartida entre varios sitios");
            if (repository.existsByEmailIgnoreCaseAndIdNot(email, id)) throw new ConflictException("Ya existe un usuario con ese correo");
            account.setEmail(email);
        }
        account.setDisplayName(request.displayName().trim());
        if (request.password() != null && !request.password().isBlank()) account.setPasswordHash(passwordEncoder.encode(request.password()));
        account.setEnabled(true);
        repository.save(account);

        membership.setRole(request.role());
        membership.setEnabled(request.enabled());
        membership = memberships.save(membership);
        promoteGlobalRole(account);
        audit.record(actor, "site.user.updated", "user", account.getId(), SiteContext.siteKey() + ":" + account.getEmail());
        return toResponse(membership);
    }

    @Transactional
    public void delete(String id, String actor) {
        UserAccount account = require(id);
        if (account.getEmail().equalsIgnoreCase(actor)) throw new BadRequestException("No puede eliminar su propia cuenta del sitio");
        if (account.isNetworkAdmin()) throw new BadRequestException("No puede retirar al administrador de la red desde Usuarios del sitio");
        CmsSiteMembership membership = memberships.findBySiteIdAndUserId(SiteContext.siteId(), id)
                .orElseThrow(() -> new NotFoundException("El usuario no pertenece a este sitio"));
        protectLastAdministrator(membership, UserRole.CUSTOMER, false);
        memberships.delete(membership);
        if (memberships.countByUserId(account.getId()) == 0) repository.delete(account);
        else promoteGlobalRole(account);
        audit.record(actor, "site.user.removed", "user", id, SiteContext.siteKey());
    }

    private void promoteGlobalRole(UserAccount account) {
        if (account.isNetworkAdmin()) { account.setRole(UserRole.ADMIN); repository.save(account); return; }
        UserRole highest = memberships.findAllByUserIdAndEnabledTrueOrderBySiteNameAsc(account.getId()).stream()
                .map(CmsSiteMembership::getRole)
                .max(Comparator.comparingInt(this::rank))
                .orElse(UserRole.CUSTOMER);
        account.setRole(highest);
        account.setEnabled(true);
        repository.save(account);
    }

    private int rank(UserRole role) {
        return switch (role) { case ADMIN -> 4; case EDITOR -> 3; case AUTHOR -> 2; case CUSTOMER -> 1; };
    }

    private void protectLastAdministrator(CmsSiteMembership current, UserRole newRole, boolean enabled) {
        boolean removes = current.getRole() == UserRole.ADMIN && current.isEnabled() && (newRole != UserRole.ADMIN || !enabled);
        if (removes && memberships.countBySiteIdAndRoleAndEnabledTrue(SiteContext.siteId(), UserRole.ADMIN) <= 1) {
            throw new BadRequestException("Debe existir al menos un administrador activo en el sitio");
        }
    }

    private UserAccount require(String id) { return repository.findById(id).orElseThrow(() -> new NotFoundException("Usuario no encontrado")); }
    private String normalizeEmail(String value) { return value.trim().toLowerCase(java.util.Locale.ROOT); }

    private ApiDtos.UserResponse toResponse(CmsSiteMembership membership) {
        UserAccount account = membership.getUser();
        return new ApiDtos.UserResponse(account.getId(), account.getEmail(), account.getDisplayName(), membership.getRole(),
                membership.isEnabled(), account.getAvatarUrl(), account.getLastLoginAt(),
                posts.countByAuthorEmailIgnoreCase(account.getEmail()), account.getCreatedAt(), account.getUpdatedAt());
    }
}

package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.domain.site.CmsSite;
import com.nattechnologies.trastcms.domain.site.CmsSiteMembership;
import com.nattechnologies.trastcms.domain.site.CmsSiteMembershipRepository;
import com.nattechnologies.trastcms.domain.site.CmsSiteRepository;
import com.nattechnologies.trastcms.domain.user.UserAccount;
import com.nattechnologies.trastcms.domain.user.UserAccountRepository;
import com.nattechnologies.trastcms.domain.user.UserRole;
import com.nattechnologies.trastcms.multisite.SiteContext;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class UserAccountService implements UserDetailsService {
    private final UserAccountRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService audit;
    private final CmsSiteRepository sites;
    private final CmsSiteMembershipRepository memberships;

    public UserAccountService(UserAccountRepository repository, PasswordEncoder passwordEncoder, AuditService audit,
                              CmsSiteRepository sites, CmsSiteMembershipRepository memberships) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
        this.sites = sites;
        this.memberships = memberships;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount account = repository.findByEmailIgnoreCase(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        return User.withUsername(account.getEmail())
                .password(account.getPasswordHash())
                .disabled(!account.isEnabled())
                .authorities(new SimpleGrantedAuthority("ROLE_" + account.getRole().name()))
                .build();
    }

    public UserAccount requireByEmail(String email) {
        return repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
    }

    @Transactional
    public UserAccount registerCustomer(String email, String displayName, String password, String phone) {
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase(java.util.Locale.ROOT);
        if (normalizedEmail.isBlank()) throw new BadRequestException("El correo es obligatorio");
        if (password == null || password.length() < 12) throw new BadRequestException("La contraseña debe tener al menos 12 caracteres");

        UserAccount account = repository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (account != null) {
            if (!passwordEncoder.matches(password, account.getPasswordHash())) {
                throw new ConflictException("Ya existe una cuenta con ese correo. Inicie sesión para vincular este sitio.");
            }
            ensureMembership(account, UserRole.CUSTOMER);
            return account;
        }

        account = new UserAccount();
        account.setEmail(normalizedEmail);
        int atIndex = normalizedEmail.indexOf('@');
        String fallbackName = atIndex > 0 ? normalizedEmail.substring(0, atIndex) : normalizedEmail;
        account.setDisplayName(displayName == null || displayName.isBlank() ? fallbackName : displayName.trim());
        account.setPhone(blank(phone));
        account.setRole(UserRole.CUSTOMER);
        account.setEnabled(true);
        account.setNetworkAdmin(false);
        account.setPasswordHash(passwordEncoder.encode(password));
        account = repository.save(account);
        ensureMembership(account, UserRole.CUSTOMER);
        audit.record(normalizedEmail, "account.customer.registered", "user", account.getId(), SiteContext.siteKey());
        return account;
    }

    private void ensureMembership(UserAccount account, UserRole role) {
        memberships.findBySiteIdAndUserId(SiteContext.siteId(), account.getId()).orElseGet(() -> {
            CmsSite site = sites.findById(SiteContext.siteId()).orElseThrow(() -> new NotFoundException("Sitio no encontrado"));
            CmsSiteMembership membership = new CmsSiteMembership();
            membership.setSite(site);
            membership.setUser(account);
            membership.setRole(role);
            membership.setEnabled(true);
            return memberships.save(membership);
        });
    }

    @Transactional
    public void changePassword(String email, String currentPassword, String newPassword) {
        UserAccount account = requireByEmail(email);
        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) throw new BadRequestException("La contraseña actual no es correcta");
        if (currentPassword.equals(newPassword)) throw new BadRequestException("La nueva contraseña debe ser diferente");
        account.setPasswordHash(passwordEncoder.encode(newPassword));
        repository.save(account);
        audit.record(email, "account.password.changed", "user", account.getId(), "Cambio de contraseña");
    }

    @Transactional(readOnly = true)
    public com.nattechnologies.trastcms.web.dto.ApiDtos.AccountProfileResponse profile(String email) {
        return profileResponse(requireByEmail(email));
    }

    @Transactional
    public com.nattechnologies.trastcms.web.dto.ApiDtos.AccountProfileResponse updateProfile(
            String email, com.nattechnologies.trastcms.web.dto.ApiDtos.AccountProfileRequest request) {
        UserAccount account = requireByEmail(email);
        account.setDisplayName(request.displayName().trim());
        account.setFirstName(blank(request.firstName()));
        account.setLastName(blank(request.lastName()));
        account.setPhone(blank(request.phone()));
        account.setAvatarUrl(blank(request.avatarUrl()));
        account.setBio(blank(request.bio()));
        account.setLocale(request.locale() == null || request.locale().isBlank() ? "es-HN" : request.locale());
        account.setTimezone(request.timezone() == null || request.timezone().isBlank() ? "America/Tegucigalpa" : request.timezone());
        repository.save(account);
        audit.record(email, "account.profile.updated", "user", account.getId(), SiteContext.siteKey());
        return profileResponse(account);
    }

    @Transactional
    public void recordLogin(String email) {
        UserAccount account = requireByEmail(email);
        account.setLastLoginAt(Instant.now());
        repository.save(account);
    }

    private com.nattechnologies.trastcms.web.dto.ApiDtos.AccountProfileResponse profileResponse(UserAccount account) {
        UserRole role = account.isNetworkAdmin() ? UserRole.ADMIN : memberships.findBySiteIdAndUserId(SiteContext.siteId(), account.getId())
                .filter(CmsSiteMembership::isEnabled).map(CmsSiteMembership::getRole).orElse(UserRole.CUSTOMER);
        return new com.nattechnologies.trastcms.web.dto.ApiDtos.AccountProfileResponse(
                account.getId(), account.getEmail(), account.getDisplayName(), account.getFirstName(),
                account.getLastName(), account.getPhone(), account.getAvatarUrl(), account.getBio(),
                account.getLocale(), account.getTimezone(), role, account.isEnabled(),
                account.getLastLoginAt(), account.getCreatedAt(), account.getUpdatedAt());
    }

    private String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}

package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.domain.user.UserAccount;
import com.nattechnologies.trastcms.domain.user.UserAccountRepository;
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

    public UserAccountService(UserAccountRepository repository, PasswordEncoder passwordEncoder, AuditService audit) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.audit = audit;
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
    public void changePassword(String email, String currentPassword, String newPassword) {
        UserAccount account = requireByEmail(email);
        if (!passwordEncoder.matches(currentPassword, account.getPasswordHash())) {
            throw new BadRequestException("La contraseña actual no es correcta");
        }
        if (currentPassword.equals(newPassword)) {
            throw new BadRequestException("La nueva contraseña debe ser diferente");
        }
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
        account.setTimezone(request.timezone() == null || request.timezone().isBlank()
                ? "America/Tegucigalpa" : request.timezone());
        repository.save(account);
        audit.record(email, "account.profile.updated", "user", account.getId(), "Perfil actualizado");
        return profileResponse(account);
    }

    @Transactional
    public void recordLogin(String email) {
        UserAccount account = requireByEmail(email);
        account.setLastLoginAt(Instant.now());
        repository.save(account);
    }

    private com.nattechnologies.trastcms.web.dto.ApiDtos.AccountProfileResponse profileResponse(UserAccount account) {
        return new com.nattechnologies.trastcms.web.dto.ApiDtos.AccountProfileResponse(
                account.getId(), account.getEmail(), account.getDisplayName(), account.getFirstName(),
                account.getLastName(), account.getPhone(), account.getAvatarUrl(), account.getBio(),
                account.getLocale(), account.getTimezone(), account.getRole(), account.isEnabled(),
                account.getLastLoginAt(), account.getCreatedAt(), account.getUpdatedAt());
    }

    private String blank(String value) { return value == null || value.isBlank() ? null : value.trim(); }

}

package com.nattechnologies.trastcms.plugins.academy;

import com.nattechnologies.trastcms.domain.user.UserAccount;
import com.nattechnologies.trastcms.plugin.api.BearerTokenAuthenticator;
import com.nattechnologies.trastcms.service.BadRequestException;
import com.nattechnologies.trastcms.service.UserAccountService;
import com.nattechnologies.trastcms.multisite.SiteAccessService;
import com.nattechnologies.trastcms.multisite.SiteContext;
import com.nattechnologies.trastcms.domain.user.UserRole;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class AcademyTokenService implements BearerTokenAuthenticator {
    private static final Duration ACCESS_DURATION = Duration.ofHours(1);
    private static final Duration REFRESH_DURATION = Duration.ofDays(30);
    private final AcademyApiTokenRepository tokens;
    private final UserAccountService users;
    private final PasswordEncoder passwords;
    private final AcademyPluginGuard guard;
    private final SiteAccessService siteAccess;
    private final SecureRandom random = new SecureRandom();

    AcademyTokenService(AcademyApiTokenRepository tokens, UserAccountService users,
                        PasswordEncoder passwords, AcademyPluginGuard guard,
                        SiteAccessService siteAccess) {
        this.tokens = tokens;
        this.users = users;
        this.passwords = passwords;
        this.guard = guard;
        this.siteAccess = siteAccess;
    }

    @Transactional
    public AcademyDtos.TokenResponse register(AcademyDtos.RegisterRequest request) {
        guard.requireEnabled();
        UserAccount account = users.registerCustomer(
                request.email(), request.displayName(), request.password(), request.phone());
        requireSiteAccess(account);
        users.recordLogin(account.getEmail());
        return issue(account, request.deviceName());
    }

    @Transactional
    public AcademyDtos.TokenResponse issue(AcademyDtos.TokenRequest request) {
        guard.requireEnabled();
        UserAccount account = users.requireByEmail(request.email());
        if (!account.isEnabled() || !passwords.matches(request.password(), account.getPasswordHash())) {
            throw new BadRequestException("Credenciales inválidas");
        }
        requireSiteAccess(account);
        users.recordLogin(account.getEmail());
        return issue(account, request.deviceName());
    }

    @Transactional
    public AcademyDtos.TokenResponse refresh(String rawRefreshToken) {
        guard.requireEnabled();
        AcademyApiToken current = tokens.findByRefreshTokenHashAndRevokedAtIsNullAndRefreshExpiresAtAfter(
                hash(rawRefreshToken), Instant.now()).orElseThrow(() -> new BadRequestException("Refresh token inválido o vencido"));
        current.revokedAt = Instant.now();
        tokens.save(current);
        UserAccount account = users.requireByEmail(current.email);
        requireSiteAccess(account);
        return issue(account, current.deviceName);
    }

    @Override
    public int order() { return 20; }

    @Override
    public boolean supports(String requestPath) {
        return requestPath != null && (requestPath.startsWith("/api/mobile/academy")
                || requestPath.startsWith("/api/compat/learnpress/v1")
                || requestPath.equals("/api/auth/token/validate")
                || requestPath.equals("/api/auth/token/revoke"));
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails authenticate(String rawAccessToken) {
        guard.requireEnabled();
        AcademyApiToken token = tokens.findByAccessTokenHashAndRevokedAtIsNullAndExpiresAtAfter(hash(rawAccessToken), Instant.now())
                .orElseThrow(() -> new BadRequestException("Token inválido o vencido"));
        UserAccount account = users.requireByEmail(token.email);
        requireSiteAccess(account);
        return users.loadUserByUsername(token.email);
    }

    @Transactional
    public AcademyDtos.TokenResponse validate(String rawAccessToken) {
        guard.requireEnabled();
        AcademyApiToken token = tokens.findByAccessTokenHashAndRevokedAtIsNullAndExpiresAtAfter(hash(rawAccessToken), Instant.now())
                .orElseThrow(() -> new BadRequestException("Token inválido o vencido"));
        token.lastUsedAt = Instant.now();
        tokens.save(token);
        UserAccount account = users.requireByEmail(token.email);
        UserRole role = requireSiteAccess(account);
        return new AcademyDtos.TokenResponse("Bearer", rawAccessToken, null,
                Math.max(0, Duration.between(Instant.now(), token.expiresAt).toSeconds()), token.expiresAt,
                token.refreshExpiresAt, account.getId(), account.getEmail(), account.getDisplayName(), role.name());
    }

    @Transactional
    public void revoke(String rawToken) {
        guard.requireEnabled();
        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException("Debe proporcionar el token a revocar");
        }
        String value = hash(rawToken.trim());
        AcademyApiToken token = tokens.findByAccessTokenHashAndRevokedAtIsNullAndExpiresAtAfter(value, Instant.now())
                .orElseGet(() -> tokens.findByRefreshTokenHashAndRevokedAtIsNullAndRefreshExpiresAtAfter(value, Instant.now())
                        .orElseThrow(() -> new BadRequestException("Token no encontrado")));
        token.revokedAt = Instant.now();
        tokens.save(token);
    }

    private AcademyDtos.TokenResponse issue(UserAccount account, String deviceName) {
        UserRole role = requireSiteAccess(account);
        String access = randomToken();
        String refresh = randomToken();
        Instant now = Instant.now();
        AcademyApiToken token = new AcademyApiToken();
        token.userId = account.getId();
        token.email = account.getEmail();
        token.deviceName = deviceName == null || deviceName.isBlank() ? "Aplicación TrastAcademy" : deviceName.trim();
        token.accessTokenHash = hash(access);
        token.refreshTokenHash = hash(refresh);
        token.expiresAt = now.plus(ACCESS_DURATION);
        token.refreshExpiresAt = now.plus(REFRESH_DURATION);
        tokens.save(token);
        return new AcademyDtos.TokenResponse("Bearer", access, refresh, ACCESS_DURATION.toSeconds(), token.expiresAt,
                token.refreshExpiresAt, account.getId(), account.getEmail(), account.getDisplayName(), role.name());
    }

    private UserRole requireSiteAccess(UserAccount account) {
        return siteAccess.role(account.getEmail(), SiteContext.siteId())
                .orElseThrow(() -> new BadRequestException("La cuenta no pertenece a este sitio"));
    }

    private String randomToken() {
        byte[] bytes = new byte[48];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String value) {
        if (value == null || value.isBlank()) throw new BadRequestException("El token es obligatorio");
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo procesar el token", exception);
        }
    }
}

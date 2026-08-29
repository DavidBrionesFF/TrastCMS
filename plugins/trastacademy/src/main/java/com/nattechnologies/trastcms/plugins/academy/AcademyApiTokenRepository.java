package com.nattechnologies.trastcms.plugins.academy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.Optional;
interface AcademyApiTokenRepository extends JpaRepository<AcademyApiToken,String> {
    Optional<AcademyApiToken> findByAccessTokenHashAndRevokedAtIsNullAndExpiresAtAfter(String hash, Instant now);
    Optional<AcademyApiToken> findByRefreshTokenHashAndRevokedAtIsNullAndRefreshExpiresAtAfter(String hash, Instant now);
}

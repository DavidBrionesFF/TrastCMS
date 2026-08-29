package com.nattechnologies.trastcms.domain.site;

import com.nattechnologies.trastcms.domain.user.UserRole;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CmsSiteMembershipRepository extends JpaRepository<CmsSiteMembership, String> {
    @EntityGraph(attributePaths = {"site", "user"})
    Optional<CmsSiteMembership> findBySiteIdAndUserId(String siteId, String userId);
    @EntityGraph(attributePaths = {"site", "user"})
    Optional<CmsSiteMembership> findBySiteIdAndUserEmailIgnoreCase(String siteId, String email);
    @EntityGraph(attributePaths = {"site", "user"})
    List<CmsSiteMembership> findAllBySiteIdOrderByUserDisplayNameAsc(String siteId);
    @EntityGraph(attributePaths = {"site", "user"})
    List<CmsSiteMembership> findAllByUserIdAndEnabledTrueOrderBySiteNameAsc(String userId);
    @EntityGraph(attributePaths = {"site", "user"})
    List<CmsSiteMembership> findAllByUserIdOrderBySiteNameAsc(String userId);
    long countBySiteIdAndRoleAndEnabledTrue(String siteId, UserRole role);
    long countBySiteIdAndEnabledTrue(String siteId);
    long countByUserId(String userId);
    void deleteBySiteIdAndUserId(String siteId, String userId);
    void deleteAllBySiteId(String siteId);
}

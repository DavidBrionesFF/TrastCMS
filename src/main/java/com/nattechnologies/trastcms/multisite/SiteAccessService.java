package com.nattechnologies.trastcms.multisite;

import com.nattechnologies.trastcms.domain.site.CmsSiteMembership;
import com.nattechnologies.trastcms.domain.site.CmsSiteMembershipRepository;
import com.nattechnologies.trastcms.domain.user.UserAccount;
import com.nattechnologies.trastcms.domain.user.UserAccountRepository;
import com.nattechnologies.trastcms.domain.user.UserRole;
import com.nattechnologies.trastcms.service.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class SiteAccessService {
    private final UserAccountRepository users;
    private final CmsSiteMembershipRepository memberships;

    public SiteAccessService(UserAccountRepository users, CmsSiteMembershipRepository memberships) {
        this.users = users;
        this.memberships = memberships;
    }

    @Transactional(readOnly = true)
    public Optional<UserRole> role(String email, String siteId) {
        if (email == null || email.isBlank()) return Optional.empty();
        UserAccount user = users.findByEmailIgnoreCase(email).orElse(null);
        if (user == null || !user.isEnabled()) return Optional.empty();
        if (user.isNetworkAdmin()) return Optional.of(UserRole.ADMIN);
        return memberships.findBySiteIdAndUserId(siteId, user.getId())
                .filter(CmsSiteMembership::isEnabled)
                .map(CmsSiteMembership::getRole);
    }

    @Transactional(readOnly = true)
    public boolean networkAdmin(String email) {
        return email != null && users.findByEmailIgnoreCase(email)
                .filter(UserAccount::isEnabled)
                .map(UserAccount::isNetworkAdmin)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public List<CmsSiteMembership> memberships(String email) {
        UserAccount user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        return memberships.findAllByUserIdAndEnabledTrueOrderBySiteNameAsc(user.getId());
    }

    public static boolean atLeast(UserRole actual, UserRole required) {
        if (actual == null) return false;
        return rank(actual) >= rank(required);
    }

    private static int rank(UserRole role) {
        return switch (role) {
            case ADMIN -> 4;
            case EDITOR -> 3;
            case AUTHOR -> 2;
            case CUSTOMER -> 1;
        };
    }
}

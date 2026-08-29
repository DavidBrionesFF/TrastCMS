package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.domain.site.CmsSite;
import com.nattechnologies.trastcms.domain.site.CmsSiteEndpoint;
import com.nattechnologies.trastcms.domain.site.CmsSiteEndpointRepository;
import com.nattechnologies.trastcms.domain.site.CmsSiteMembership;
import com.nattechnologies.trastcms.domain.site.CmsSiteMembershipRepository;
import com.nattechnologies.trastcms.domain.site.CmsSiteRepository;
import com.nattechnologies.trastcms.domain.user.UserAccount;
import com.nattechnologies.trastcms.domain.user.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Persists the network-level site registry separately from tenant-owned data.
 *
 * <p>Tenant provisioning must start only after this transaction commits because
 * tenant tables reference {@code cms_site}. If provisioning subsequently fails,
 * {@link #removeUnprovisioned(String)} compensates the committed registry rows.
 * This keeps a failed theme/plugin blueprint from leaving a half-created site in
 * the network dashboard.</p>
 */
@Service
public class SiteRegistryOperations {
    private final CmsSiteRepository sites;
    private final CmsSiteEndpointRepository endpoints;
    private final CmsSiteMembershipRepository memberships;

    public SiteRegistryOperations(CmsSiteRepository sites,
                                  CmsSiteEndpointRepository endpoints,
                                  CmsSiteMembershipRepository memberships) {
        this.sites = sites;
        this.endpoints = endpoints;
        this.memberships = memberships;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CmsSite register(CmsSite site, List<EndpointSeed> endpointSeeds, UserAccount creator) {
        CmsSite saved = sites.saveAndFlush(site);
        for (EndpointSeed seed : endpointSeeds) {
            CmsSiteEndpoint endpoint = new CmsSiteEndpoint();
            endpoint.setSite(saved);
            endpoint.setHost(seed.host());
            endpoint.setPort(seed.port());
            endpoint.setScheme(seed.scheme());
            endpoint.setPrimaryEndpoint(seed.primaryEndpoint());
            endpoint.setActive(seed.active());
            endpoints.save(endpoint);
        }

        CmsSiteMembership membership = new CmsSiteMembership();
        membership.setSite(saved);
        membership.setUser(creator);
        membership.setRole(UserRole.ADMIN);
        membership.setEnabled(true);
        memberships.save(membership);
        memberships.flush();
        return saved;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void removeUnprovisioned(String siteId) {
        memberships.deleteAllBySiteId(siteId);
        memberships.flush();
        endpoints.deleteAllBySiteId(siteId);
        endpoints.flush();
        sites.deleteById(siteId);
        sites.flush();
    }

    public record EndpointSeed(String host, int port, String scheme, boolean primaryEndpoint, boolean active) {}
}

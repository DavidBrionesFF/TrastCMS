package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.config.TrastCmsProperties;
import com.nattechnologies.trastcms.domain.site.*;
import com.nattechnologies.trastcms.domain.user.UserAccount;
import com.nattechnologies.trastcms.domain.user.UserAccountRepository;
import com.nattechnologies.trastcms.domain.user.UserRole;
import com.nattechnologies.trastcms.multisite.SiteContext;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Locale;

@Component
@Order(30)
public class MultisiteBootstrapService implements ApplicationRunner {
    private final CmsSiteRepository sites;
    private final CmsSiteMembershipRepository memberships;
    private final CmsSiteEndpointRepository endpoints;
    private final UserAccountRepository users;
    private final TrastCmsProperties properties;
    private final SiteProvisioningService provisioning;

    public MultisiteBootstrapService(CmsSiteRepository sites, CmsSiteMembershipRepository memberships,
                                     CmsSiteEndpointRepository endpoints, UserAccountRepository users,
                                     TrastCmsProperties properties, SiteProvisioningService provisioning) {
        this.sites = sites;
        this.memberships = memberships;
        this.endpoints = endpoints;
        this.users = users;
        this.properties = properties;
        this.provisioning = provisioning;
    }

    @Override
    public void run(ApplicationArguments args) {
        UserAccount networkAdmin = users.findByEmailIgnoreCase(properties.getAdmin().getEmail()).orElse(null);
        sites.findById(SiteContext.PRIMARY_SITE_ID).ifPresent(this::ensureConfiguredBaseUrl);
        for (CmsSite site : sites.findAllByOrderByNameAsc()) {
            if (networkAdmin != null) memberships.findBySiteIdAndUserId(site.getId(), networkAdmin.getId()).orElseGet(() -> {
                CmsSiteMembership membership = new CmsSiteMembership();
                membership.setSite(site);
                membership.setUser(networkAdmin);
                membership.setRole(UserRole.ADMIN);
                membership.setEnabled(true);
                return memberships.save(membership);
            });
            if (SiteContext.ACADEMY_SITE_ID.equals(site.getId())) provisioning.provisionIfEmpty(site, "ACADEMY", properties.getAdmin().getEmail());
            else if (SiteContext.SAAS_SITE_ID.equals(site.getId())) provisioning.provisionIfEmpty(site, "SAAS", properties.getAdmin().getEmail());
        }
    }
    private void ensureConfiguredBaseUrl(CmsSite primarySite) {
        String configured = properties.getBaseUrl();
        if (configured == null || configured.isBlank()) return;
        try {
            URI uri = URI.create(configured.trim());
            String host = uri.getHost();
            if (host == null || host.isBlank()) return;
            host = host.toLowerCase(Locale.ROOT);
            String scheme = uri.getScheme() == null ? "https" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!"http".equals(scheme) && !"https".equals(scheme)) return;
            int port = uri.getPort() > 0 ? uri.getPort() : ("https".equals(scheme) ? 443 : 80);

            // Preserve the local development endpoint, but prefer a configured
            // public URL as the canonical link shown by the network dashboard.
            boolean publicEndpoint = !("localhost".equals(host) || "127.0.0.1".equals(host));
            var existing = endpoints.findFirstByHostIgnoreCaseAndPort(host, port);
            if (existing.isPresent()) {
                CmsSiteEndpoint current = existing.get();
                if (publicEndpoint && current.getSite().getId().equals(primarySite.getId()) && !current.isPrimaryEndpoint()) {
                    setPrimaryEndpoint(primarySite, current);
                }
                return;
            }

            if (publicEndpoint) setPrimaryEndpoint(primarySite, null);

            CmsSiteEndpoint endpoint = new CmsSiteEndpoint();
            endpoint.setSite(primarySite);
            endpoint.setHost(host);
            endpoint.setPort(port);
            endpoint.setScheme(scheme);
            endpoint.setPrimaryEndpoint(publicEndpoint);
            endpoint.setActive(true);
            endpoints.save(endpoint);
        } catch (IllegalArgumentException ignored) {
            // A malformed legacy base URL should not prevent the CMS from
            // starting; localhost remains available for network administration.
        }
    }

    private void setPrimaryEndpoint(CmsSite site, CmsSiteEndpoint selected) {
        for (CmsSiteEndpoint endpoint : endpoints.findAllBySiteIdOrderByPrimaryEndpointDescHostAsc(site.getId())) {
            boolean primary = selected != null && endpoint.getId().equals(selected.getId());
            if (endpoint.isPrimaryEndpoint() != primary) {
                endpoint.setPrimaryEndpoint(primary);
                endpoints.save(endpoint);
            }
        }
        if (selected != null && !selected.isPrimaryEndpoint()) {
            selected.setPrimaryEndpoint(true);
            endpoints.save(selected);
        }
    }

}

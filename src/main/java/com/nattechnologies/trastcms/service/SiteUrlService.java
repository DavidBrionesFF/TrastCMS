package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.config.TrastCmsProperties;
import com.nattechnologies.trastcms.domain.site.CmsSiteEndpoint;
import com.nattechnologies.trastcms.domain.site.CmsSiteEndpointRepository;
import com.nattechnologies.trastcms.multisite.SiteContext;
import org.springframework.stereotype.Service;

import java.net.URI;

/** Resolves the canonical public URL for the current tenant. */
@Service
public class SiteUrlService {
    private final CmsSiteEndpointRepository endpoints;
    private final TrastCmsProperties properties;

    public SiteUrlService(CmsSiteEndpointRepository endpoints, TrastCmsProperties properties) {
        this.endpoints = endpoints;
        this.properties = properties;
    }

    public String currentBaseUrl() {
        return endpoints.findFirstBySiteIdAndPrimaryEndpointTrueAndActiveTrue(SiteContext.siteId())
                .map(this::url)
                .orElseGet(() -> endpoints.findAllBySiteIdOrderByPrimaryEndpointDescHostAsc(SiteContext.siteId()).stream()
                        .filter(CmsSiteEndpoint::isActive)
                        .findFirst()
                        .map(this::url)
                        .orElseGet(this::safeFallbackBaseUrl));
    }

    private String url(CmsSiteEndpoint endpoint) {
        int defaultPort = "https".equalsIgnoreCase(endpoint.getScheme()) ? 443 : 80;
        String port = endpoint.getPort() == null || endpoint.getPort() == defaultPort ? "" : ":" + endpoint.getPort();
        return URI.create(endpoint.getScheme() + "://" + endpoint.getHost() + port).toString().replaceAll("/+$", "");
    }

    private String safeFallbackBaseUrl() {
        // TRASTCMS_BASE_URL is the legacy/default-site URL. Never advertise it
        // as the canonical URL of another tenant when that tenant has no active
        // endpoint; a relative URL is safer than crossing tenant boundaries.
        if (!SiteContext.DEFAULT_SITE_ID.equals(SiteContext.siteId())) return "";
        String value = properties.getBaseUrl();
        return value == null || value.isBlank() ? "http://localhost:8080" : value.replaceAll("/+$", "");
    }
}

package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.domain.site.*;
import com.nattechnologies.trastcms.domain.user.UserAccount;
import com.nattechnologies.trastcms.domain.user.UserAccountRepository;
import com.nattechnologies.trastcms.domain.user.UserRole;
import com.nattechnologies.trastcms.multisite.SiteAccessService;
import com.nattechnologies.trastcms.multisite.SiteContext;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import org.springframework.stereotype.Service;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.util.*;

@Service
public class SiteService {
    private final CmsSiteRepository sites;
    private final CmsSiteEndpointRepository endpoints;
    private final CmsSiteMembershipRepository memberships;
    private final UserAccountRepository users;
    private final SiteAccessService access;
    private final SiteProvisioningService provisioning;
    private final SiteRegistryOperations registry;
    private final SiteSettingService settings;
    private final BundledPluginService plugins;
    private final AuditService audit;

    public SiteService(CmsSiteRepository sites, CmsSiteEndpointRepository endpoints,
                       CmsSiteMembershipRepository memberships, UserAccountRepository users,
                       SiteAccessService access, SiteProvisioningService provisioning,
                       SiteRegistryOperations registry, SiteSettingService settings,
                       BundledPluginService plugins, AuditService audit) {
        this.sites = sites;
        this.endpoints = endpoints;
        this.memberships = memberships;
        this.users = users;
        this.access = access;
        this.provisioning = provisioning;
        this.registry = registry;
        this.settings = settings;
        this.plugins = plugins;
        this.audit = audit;
    }

    public List<ApiDtos.SiteResponse> list(String actor) {
        List<CmsSite> visible;
        if (access.networkAdmin(actor)) visible = sites.findAllByOrderByNameAsc();
        else visible = access.memberships(actor).stream().map(CmsSiteMembership::getSite).toList();
        return visible.stream().filter(site -> site.getStatus() != SiteStatus.ARCHIVED || access.networkAdmin(actor))
                .map(this::response).toList();
    }

    public List<ApiDtos.SiteMembershipSummary> available(String actor) {
        UserAccount user = users.findByEmailIgnoreCase(actor).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        if (user.isNetworkAdmin()) {
            return sites.findAllByOrderByNameAsc().stream()
                    .filter(site -> site.getStatus() != SiteStatus.ARCHIVED)
                    .map(site -> new ApiDtos.SiteMembershipSummary(site.getId(), site.getSiteKey(), site.getName(), "ADMIN", primaryUrl(site.getId())))
                    .toList();
        }
        return memberships.findAllByUserIdAndEnabledTrueOrderBySiteNameAsc(user.getId()).stream()
                .filter(item -> item.getSite().getStatus() != SiteStatus.ARCHIVED)
                .map(item -> new ApiDtos.SiteMembershipSummary(item.getSite().getId(), item.getSite().getSiteKey(), item.getSite().getName(), item.getRole().name(), primaryUrl(item.getSite().getId())))
                .toList();
    }

    public ApiDtos.SiteResponse current() {
        return response(require(SiteContext.siteId()));
    }

    public ApiDtos.SiteResponse create(ApiDtos.SiteRequest request, String actor) {
        requireNetworkAdmin(actor);
        String key = normalizeKey(request.siteKey());
        if (sites.existsBySiteKeyIgnoreCase(key)) throw new ConflictException("Ya existe un sitio con esa clave");

        CmsSite site = new CmsSite();
        site.setSiteKey(key);
        site.setName(request.name().trim());
        site.setLocale(request.locale());
        site.setTimezone(request.timezone());
        site.setStatus(status(request.status(), SiteStatus.ACTIVE));

        List<ApiDtos.SiteEndpointRequest> endpointValues = endpointValues(site, request.endpoints());
        validateEndpoints(site, endpointValues);
        List<SiteRegistryOperations.EndpointSeed> seeds = endpointSeeds(endpointValues);
        UserAccount creator = users.findByEmailIgnoreCase(actor)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        site = registry.register(site, seeds, creator);
        try {
            provisioning.provision(site, request.blueprint(), actor);
        } catch (RuntimeException failure) {
            try {
                registry.removeUnprovisioned(site.getId());
            } catch (RuntimeException cleanupFailure) {
                failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }

        audit.record(actor, "site.created", "site", site.getId(), site.getSiteKey());
        return response(site);
    }

    @Transactional
    public ApiDtos.SiteResponse update(String id, ApiDtos.SiteRequest request, String actor) {
        requireNetworkAdmin(actor);
        CmsSite site = require(id);
        String key = normalizeKey(request.siteKey());
        sites.findBySiteKeyIgnoreCase(key).filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new ConflictException("Ya existe un sitio con esa clave"); });
        site.setSiteKey(key);
        site.setName(request.name().trim());
        site.setLocale(request.locale());
        site.setTimezone(request.timezone());
        site.setStatus(status(request.status(), site.getStatus()));
        site = sites.save(site);
        if (request.endpoints() != null) {
            validateEndpoints(site, request.endpoints());
            endpoints.deleteAllBySiteId(site.getId());
            saveEndpoints(site, request.endpoints());
        }
        CmsSite finalSite = site;
        SiteContext.withSite(resolved(site), () -> {
            settings.put("site.name", finalSite.getName());
            settings.put("site.locale", finalSite.getLocale());
            settings.put("site.timezone", finalSite.getTimezone());
            settings.put("site.suspended", String.valueOf(finalSite.getStatus() == SiteStatus.SUSPENDED));
        });
        audit.record(actor, "site.updated", "site", site.getId(), site.getSiteKey());
        return response(site);
    }

    public ApiDtos.SiteResponse configurePlugins(String id, ApiDtos.PluginSelectionRequest request, String actor) {
        requireNetworkAdmin(actor);
        CmsSite site = require(id);
        provisioning.configurePlugins(site, new LinkedHashSet<>(request.enabledPluginIds() == null ? List.of() : request.enabledPluginIds()), actor);
        return response(site);
    }

    public ApiDtos.SiteResponse reprovision(String id, String blueprint, String actor) {
        requireNetworkAdmin(actor);
        CmsSite site = require(id);
        provisioning.provision(site, blueprint, actor);
        audit.record(actor, "site.reprovisioned", "site", site.getId(), blueprint == null ? "EMPTY" : blueprint);
        return response(site);
    }

    private List<SiteRegistryOperations.EndpointSeed> endpointSeeds(List<ApiDtos.SiteEndpointRequest> values) {
        int primaryIndex = primaryIndex(values);
        List<SiteRegistryOperations.EndpointSeed> result = new ArrayList<>();
        for (int i = 0; i < values.size(); i++) {
            ApiDtos.SiteEndpointRequest item = values.get(i);
            result.add(new SiteRegistryOperations.EndpointSeed(
                    normalizeHost(item.host()),
                    item.port(),
                    item.scheme() == null || item.scheme().isBlank() ? "https" : item.scheme().toLowerCase(Locale.ROOT),
                    i == primaryIndex,
                    item.active()));
        }
        return result;
    }

    private void saveEndpoints(CmsSite site, List<ApiDtos.SiteEndpointRequest> input) {
        List<ApiDtos.SiteEndpointRequest> values = endpointValues(site, input);
        validateEndpoints(site, values);
        int primaryIndex = primaryIndex(values);
        for (int i = 0; i < values.size(); i++) {
            ApiDtos.SiteEndpointRequest item = values.get(i);
            String host = normalizeHost(item.host());
            Integer port = item.port();
            CmsSiteEndpoint endpoint = new CmsSiteEndpoint();
            endpoint.setSite(site);
            endpoint.setHost(host);
            endpoint.setPort(port);
            endpoint.setScheme(item.scheme() == null || item.scheme().isBlank() ? "https" : item.scheme().toLowerCase(Locale.ROOT));
            endpoint.setPrimaryEndpoint(i == primaryIndex);
            endpoint.setActive(item.active());
            endpoints.save(endpoint);
        }
    }


    private List<ApiDtos.SiteEndpointRequest> endpointValues(CmsSite site, List<ApiDtos.SiteEndpointRequest> input) {
        return input == null || input.isEmpty()
                ? List.of(new ApiDtos.SiteEndpointRequest(site.getSiteKey() + ".localhost", 8080, "http", true, true))
                : input;
    }

    private void validateEndpoints(CmsSite site, List<ApiDtos.SiteEndpointRequest> input) {
        List<ApiDtos.SiteEndpointRequest> values = endpointValues(site, input);
        long primaryCount = values.stream().filter(ApiDtos.SiteEndpointRequest::primaryEndpoint).count();
        if (primaryCount > 1) throw new BadRequestException("Solo puede existir un endpoint principal por sitio");
        if (values.stream().anyMatch(item -> item.primaryEndpoint() && !item.active())) {
            throw new BadRequestException("El endpoint principal debe estar activo");
        }
        if (site.getStatus() == SiteStatus.ACTIVE && values.stream().noneMatch(ApiDtos.SiteEndpointRequest::active)) {
            throw new BadRequestException("Un sitio activo necesita al menos un dominio o puerto activo");
        }
        Set<String> seen = new HashSet<>();
        for (ApiDtos.SiteEndpointRequest item : values) {
            String host = normalizeHost(item.host());
            Integer port = item.port();
            String key = host + ":" + (port == null ? "" : port);
            if (!seen.add(key)) throw new BadRequestException("El dominio o puerto está repetido en el sitio: " + key);
            endpoints.findFirstByHostIgnoreCaseAndPort(host, port)
                    .filter(existing -> !existing.getSite().getId().equals(site.getId()))
                    .ifPresent(existing -> { throw new ConflictException("El dominio o puerto ya pertenece a otro sitio: " + key); });
        }
    }

    private int primaryIndex(List<ApiDtos.SiteEndpointRequest> values) {
        for (int i = 0; i < values.size(); i++) if (values.get(i).primaryEndpoint()) return i;
        for (int i = 0; i < values.size(); i++) if (values.get(i).active()) return i;
        return 0;
    }

    private ApiDtos.SiteResponse response(CmsSite site) {
        List<ApiDtos.SiteEndpointResponse> endpointResponses = endpoints.findAllBySiteIdOrderByPrimaryEndpointDescHostAsc(site.getId()).stream()
                .map(item -> new ApiDtos.SiteEndpointResponse(item.getId(), item.getHost(), item.getPort(), item.getScheme(), item.isPrimaryEndpoint(), item.isActive()))
                .toList();
        Holder holder = SiteContext.withSite(resolved(site), () -> new Holder(
                settings.get("theme.active", "nexus"),
                new ArrayList<>(plugins.enabledIds())));
        return new ApiDtos.SiteResponse(site.getId(), site.getSiteKey(), site.getName(), site.getStatus().name(),
                site.getLocale(), site.getTimezone(), primaryUrl(site.getId()), holder.theme(), holder.plugins(),
                memberships.findAllBySiteIdOrderByUserDisplayNameAsc(site.getId()).stream().filter(CmsSiteMembership::isEnabled).count(),
                site.getId().equals(SiteContext.siteId()), site.getCreatedAt(), site.getUpdatedAt(), endpointResponses);
    }

    private String primaryUrl(String siteId) {
        return endpoints.findFirstBySiteIdAndPrimaryEndpointTrueAndActiveTrue(siteId)
                .map(endpoint -> {
                    int defaultPort = "https".equalsIgnoreCase(endpoint.getScheme()) ? 443 : 80;
                    String port = endpoint.getPort() == null || endpoint.getPort() == defaultPort ? "" : ":" + endpoint.getPort();
                    return URI.create(endpoint.getScheme() + "://" + endpoint.getHost() + port).toString();
                }).orElse("");
    }

    private CmsSite require(String id) {
        return sites.findById(id).orElseThrow(() -> new NotFoundException("Sitio no encontrado"));
    }

    private void requireNetworkAdmin(String actor) {
        if (!access.networkAdmin(actor)) throw new AccessDeniedException("Solo el administrador de la red puede modificar sitios");
    }

    private String normalizeKey(String value) { return value.trim().toLowerCase(Locale.ROOT); }
    private String normalizeHost(String value) {
        String host = value.trim().toLowerCase(Locale.ROOT);
        if (host.contains("://") || host.contains("/") || host.contains(" ")) throw new BadRequestException("Escriba únicamente el host, sin protocolo ni ruta");
        return host;
    }
    private SiteStatus status(String value, SiteStatus fallback) {
        if (value == null || value.isBlank()) return fallback;
        try { return SiteStatus.valueOf(value); } catch (IllegalArgumentException ex) { throw new BadRequestException("Estado de sitio inválido"); }
    }
    private SiteContext.ResolvedSite resolved(CmsSite site) { return new SiteContext.ResolvedSite(site.getId(), site.getSiteKey(), site.getName()); }
    private record Holder(String theme, List<String> plugins) {}
}

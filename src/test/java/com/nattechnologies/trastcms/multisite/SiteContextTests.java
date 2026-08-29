package com.nattechnologies.trastcms.multisite;

import com.nattechnologies.trastcms.domain.user.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SiteContextTests {

    @AfterEach
    void clear() {
        SiteContext.clear();
    }

    @Test
    void defaultsToTourPrimaryOutsideARequest() {
        assertEquals(SiteContext.PRIMARY_SITE_ID, SiteContext.siteId());
        assertEquals("primary-site", SiteContext.siteKey());
        assertEquals("Primary Site", SiteContext.siteName());
    }

    @Test
    void nestedSiteScopesRestoreThePreviousTenant() {
        SiteContext.ResolvedSite primary = new SiteContext.ResolvedSite(SiteContext.PRIMARY_SITE_ID, "primary-site", "Primary Site");
        SiteContext.ResolvedSite academy = new SiteContext.ResolvedSite(SiteContext.ACADEMY_SITE_ID, "academy", "Academy");

        SiteContext.withSite(primary, () -> {
            assertEquals("primary-site", SiteContext.siteKey());
            SiteContext.withSite(academy, () -> assertEquals("academy", SiteContext.siteKey()));
            assertEquals("primary-site", SiteContext.siteKey());
        });
        assertTrue(SiteContext.current().isEmpty());
    }


    @Test
    void asyncTaskDecoratorCarriesAndRestoresTenantContext() {
        MultisiteAsyncConfiguration configuration = new MultisiteAsyncConfiguration();
        var decorator = configuration.siteContextTaskDecorator();
        SiteContext.ResolvedSite academy = new SiteContext.ResolvedSite(
                SiteContext.ACADEMY_SITE_ID, "academy", "Academy");
        SiteContext.ResolvedSite primary = new SiteContext.ResolvedSite(
                SiteContext.PRIMARY_SITE_ID, "primary-site", "Primary Site");

        SiteContext.set(academy);
        Runnable decorated = decorator.decorate(() -> assertEquals("academy", SiteContext.siteKey()));

        // Simulate execution on a worker that already has another context.
        SiteContext.set(primary);
        decorated.run();
        assertEquals("primary-site", SiteContext.siteKey());
    }

    @Test
    void roleHierarchyMatchesAdminEditorAuthorCustomer() {
        assertTrue(SiteAccessService.atLeast(UserRole.ADMIN, UserRole.CUSTOMER));
        assertTrue(SiteAccessService.atLeast(UserRole.EDITOR, UserRole.AUTHOR));
        assertTrue(SiteAccessService.atLeast(UserRole.AUTHOR, UserRole.AUTHOR));
        assertFalse(SiteAccessService.atLeast(UserRole.CUSTOMER, UserRole.AUTHOR));
        assertFalse(SiteAccessService.atLeast(null, UserRole.CUSTOMER));
    }
}

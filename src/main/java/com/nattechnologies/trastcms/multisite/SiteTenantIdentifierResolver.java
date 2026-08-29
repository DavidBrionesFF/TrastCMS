package com.nattechnologies.trastcms.multisite;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

@Component
public class SiteTenantIdentifierResolver implements CurrentTenantIdentifierResolver<String> {
    @Override
    public String resolveCurrentTenantIdentifier() {
        return SiteContext.siteId();
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return true;
    }
}

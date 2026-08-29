package com.nattechnologies.trastcms.multisite;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MultisiteFilterConfiguration {
    @Bean
    FilterRegistrationBean<SiteResolutionFilter> disableSiteResolutionServletRegistration(SiteResolutionFilter filter) {
        FilterRegistrationBean<SiteResolutionFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    FilterRegistrationBean<SiteMembershipAuthorizationFilter> disableSiteMembershipServletRegistration(SiteMembershipAuthorizationFilter filter) {
        FilterRegistrationBean<SiteMembershipAuthorizationFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}

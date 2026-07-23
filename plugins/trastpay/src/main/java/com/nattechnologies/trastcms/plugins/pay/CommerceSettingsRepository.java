package com.nattechnologies.trastcms.plugins.pay;

import org.springframework.data.jpa.repository.JpaRepository;

interface CommerceSettingsRepository
        extends JpaRepository<CommerceSettings, String> {
}

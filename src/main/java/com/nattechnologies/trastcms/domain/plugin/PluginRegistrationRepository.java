package com.nattechnologies.trastcms.domain.plugin;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PluginRegistrationRepository extends JpaRepository<PluginRegistration, String> {
    Optional<PluginRegistration> findByPluginKey(String pluginKey);
    List<PluginRegistration> findByEnabledTrue();
}

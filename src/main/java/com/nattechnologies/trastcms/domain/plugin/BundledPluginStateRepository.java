package com.nattechnologies.trastcms.domain.plugin;

import com.nattechnologies.trastcms.multisite.SiteContext;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BundledPluginStateRepository extends JpaRepository<BundledPluginState, String> {
    default String currentKey(String pluginId) { return SiteContext.siteId() + "::" + pluginId; }
    default Optional<BundledPluginState> findCurrent(String pluginId) { return findById(currentKey(pluginId)); }
}

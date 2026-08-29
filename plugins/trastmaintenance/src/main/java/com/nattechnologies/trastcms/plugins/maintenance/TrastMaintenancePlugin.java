package com.nattechnologies.trastcms.plugins.maintenance;

import com.nattechnologies.trastcms.plugin.api.BundledPlugin;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class TrastMaintenancePlugin implements BundledPlugin {

    @Override
    public String id() {
        return "trastmaintenance";
    }

    @Override
    public String name() {
        return "TrastMaintenance";
    }

    @Override
    public String version() {
        return "1.0.0";
    }

    @Override
    public String description() {
        return "Backups, caché y salud del sistema.";
    }

    @Override
    public boolean enabledByDefault() {
        return true;
    }

    @Override
    public List<String> capabilities() {
        return List.of("maintenance.backup", "maintenance.cache", "maintenance.health", "maintenance.gdrive");
    }

    @Override
    public List<Map<String, Object>> adminMenuItems() {
        return List.of(
            Map.of(
                "id", "maintenance",
                "label", "Mantenimiento",
                "icon", "shield-check",
                "description", "Backups, caché y optimización.",
                "routeName", "maintenance"
            )
        );
    }
}

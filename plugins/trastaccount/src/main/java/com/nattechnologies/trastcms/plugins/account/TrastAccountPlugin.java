package com.nattechnologies.trastcms.plugins.account;

import com.nattechnologies.trastcms.plugin.api.BundledPlugin;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class TrastAccountPlugin implements BundledPlugin {
    @Override public String id() { return "trastaccount"; }
    @Override public String name() { return "TrastAccount"; }
    @Override public String version() { return "1.0.0"; }
    @Override public boolean enabledByDefault() { return true; }
    @Override public String description() {
        return "Portal Mi Cuenta para registro de clientes, perfil e integraciones con compras, licencias y viajes.";
    }
    @Override public List<String> optionalPlugins() { return List.of("trastpay", "traststore", "trastsaas", "trasttravel"); }
    @Override
    public List<String> capabilities() {
        return List.of(
                "account.registration", "account.profile", "account.password",
                "account.sections", "account.plugin-integrations", "rest.api");
    }
    @Override
    public List<Map<String, Object>> adminMenuItems() {
        return List.of(Map.of(
                "id", "account-portal",
                "label", "Portal Mi Cuenta",
                "icon", "account",
                "description", "Registro de clientes e integraciones del portal.",
                "routeName", "account-portal-admin"));
    }
}

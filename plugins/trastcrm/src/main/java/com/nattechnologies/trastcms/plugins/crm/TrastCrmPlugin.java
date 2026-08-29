package com.nattechnologies.trastcms.plugins.crm;

import com.nattechnologies.trastcms.plugin.api.BundledPlugin;
import com.nattechnologies.trastcms.plugin.api.PluginEvent;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class TrastCrmPlugin implements BundledPlugin {
    private final CrmService crm;
    private final CrmReputationService reputation;
    public TrastCrmPlugin(CrmService crm, CrmReputationService reputation) {
        this.crm = crm;
        this.reputation = reputation;
    }
    @Override public String id() { return "trastcrm"; }
    @Override public String name() { return "TrastCRM"; }
    @Override public String version() { return "1.6.0"; }
    @Override public String description() { return "CRM integrado con contactos, oportunidades, formularios y automatización de reputación para Travel, Academy y SaaS."; }
    @Override public boolean enabledByDefault() { return true; }
    @Override public List<String> capabilities() { return List.of("crm.contacts","crm.companies","crm.pipeline","crm.activities","crm.forms","crm.reputation","builder.blocks","public.forms"); }
    @Override public List<String> optionalPlugins() { return List.of("trasttravel","trastacademy","trastsaas"); }
    @Override public void onEnabled() { crm.initializeDefaults(); }
    @Override public void onEvent(PluginEvent event) {
        crm.captureTravelEvent(event);
        reputation.handleEvent(event);
    }

    @Override
    public List<Map<String, Object>> blocks() {
        Map<String,Object> schema = new LinkedHashMap<>();
        schema.put("formKey", Map.of("type","select","label","Formulario","default","contacto",
                "optionsEndpoint","/api/admin/crm/forms","optionsLabel","name","optionsValue","formKey"));
        schema.put("title", Map.of("type","text","label","Título","default","Contáctanos"));
        schema.put("showTitle", Map.of("type","boolean","label","Mostrar título","default",true));
        schema.put("style", Map.of("type","select","label","Estilo","default","card","options",List.of(
                Map.of("label","Tarjeta","value","card"), Map.of("label","Minimalista","value","minimal"), Map.of("label","Oscuro","value","dark"))));
        return List.of(Map.of(
                "type","form","label","Formulario CRM","description","Inserte un formulario creado en TrastCRM.",
                "category","CRM","icon","form","template","<div class=\"rounded-xl border border-violet-200 bg-violet-50 p-6\">Formulario CRM: {{formKey}}</div>",
                "schema",schema));
    }

    @Override
    public List<Map<String, Object>> adminMenuItems() {
        return List.of(Map.of("id","crm","label","CRM","icon","crm","description","Contactos, ventas, actividades, formularios y reputación.","routeName","crm"));
    }
}

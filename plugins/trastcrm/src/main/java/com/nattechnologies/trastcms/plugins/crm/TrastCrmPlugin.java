package com.nattechnologies.trastcms.plugins.crm;

import com.nattechnologies.trastcms.plugin.api.BundledPlugin;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class TrastCrmPlugin implements BundledPlugin {
    private final CrmService crm;
    public TrastCrmPlugin(CrmService crm) { this.crm = crm; }
    @Override public String id() { return "trastcrm"; }
    @Override public String name() { return "TrastCRM"; }
    @Override public String version() { return "1.3.0"; }
    @Override public String description() { return "CRM integrado con contactos, empresas, oportunidades, actividades, formularios y captación de leads."; }
    @Override public boolean enabledByDefault() { return true; }
    @Override public List<String> capabilities() { return List.of("crm.contacts","crm.companies","crm.pipeline","crm.activities","crm.forms","builder.blocks","public.forms"); }
    @Override public void onEnabled() { crm.initializeDefaults(); }

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
        return List.of(Map.of("id","crm","label","CRM","icon","crm","description","Contactos, ventas, actividades y formularios.","routeName","crm"));
    }
}

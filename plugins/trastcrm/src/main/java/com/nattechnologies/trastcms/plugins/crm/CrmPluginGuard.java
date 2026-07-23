package com.nattechnologies.trastcms.plugins.crm;
import com.nattechnologies.trastcms.domain.plugin.BundledPluginStateRepository; import com.nattechnologies.trastcms.service.NotFoundException; import org.springframework.stereotype.Component;
@Component class CrmPluginGuard { private final BundledPluginStateRepository states; CrmPluginGuard(BundledPluginStateRepository states){this.states=states;} void requireEnabled(){if(!states.findById("trastcrm").map(s->s.isEnabled()).orElse(false))throw new NotFoundException("TrastCRM está desactivado");} }

package com.nattechnologies.trastcms.plugins.academy;
import com.nattechnologies.trastcms.domain.plugin.BundledPluginStateRepository;
import com.nattechnologies.trastcms.service.NotFoundException;
import org.springframework.stereotype.Component;
@Component
class AcademyPluginGuard {
    private final BundledPluginStateRepository states;
    AcademyPluginGuard(BundledPluginStateRepository states){this.states=states;}
    void requireEnabled(){if(!states.findCurrent("trastacademy").map(s->s.isEnabled()).orElse(false)) throw new NotFoundException("TrastAcademy está desactivado");}
    boolean enabled(){return states.findCurrent("trastacademy").map(s->s.isEnabled()).orElse(false);}
}

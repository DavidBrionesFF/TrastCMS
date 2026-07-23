package com.nattechnologies.trastcms.plugins.pay;
import com.nattechnologies.trastcms.domain.plugin.BundledPluginStateRepository; import com.nattechnologies.trastcms.service.NotFoundException; import org.springframework.stereotype.Component;
@Component class PayPluginGuard { private final BundledPluginStateRepository states; PayPluginGuard(BundledPluginStateRepository states){this.states=states;} void requireEnabled(){if(!states.findById("trastpay").map(s->s.isEnabled()).orElse(false))throw new NotFoundException("TrastPay está desactivado");} }

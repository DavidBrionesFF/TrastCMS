package com.nattechnologies.trastcms.plugins.travel;

import com.nattechnologies.trastcms.plugin.api.AccountSectionProvider;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.Map;

@Component
public class TravelAccountSectionProvider implements AccountSectionProvider {
    private final TravelService service;

    TravelAccountSectionProvider(TravelService service) {
        this.service = service;
    }

    @Override public String key() { return "travel"; }
    @Override public String pluginId() { return "trasttravel"; }
    @Override public String label() { return "Mis viajes"; }
    @Override public String icon() { return "calendar"; }
    @Override public int order() { return 20; }

    @Override
    public AccountSection load(AccountContext context) {
        List<Map<String, Object>> items = service.accountBookings(context.email());
        long pending = items.stream()
                .filter(item -> "PENDING_PAYMENT".equals(item.get("status")))
                .count();
        long confirmed = items.stream()
                .filter(item -> "CONFIRMED".equals(item.get("status")))
                .count();
        return new AccountSection(
                key(), pluginId(), label(), icon(), order(), "READY",
                "Todavía no tiene viajes reservados.",
                items,
                Map.of("total", items.size(), "pending", pending, "confirmed", confirmed));
    }
}

package com.nattechnologies.trastcms.plugins.account;

import com.nattechnologies.trastcms.domain.user.UserAccount;
import com.nattechnologies.trastcms.plugin.api.AccountSectionProvider;
import com.nattechnologies.trastcms.service.BundledPluginService;
import com.nattechnologies.trastcms.service.UserAccountService;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AccountService {
    private final UserAccountService users;
    private final BundledPluginService plugins;
    private final List<AccountSectionProvider> providers;
    private final AccountPluginGuard guard;

    AccountService(
            UserAccountService users,
            BundledPluginService plugins,
            List<AccountSectionProvider> providers,
            AccountPluginGuard guard) {
        this.users = users;
        this.plugins = plugins;
        this.providers = providers.stream()
                .sorted(Comparator.comparingInt(AccountSectionProvider::order))
                .toList();
        this.guard = guard;
    }

    @Transactional
    public ApiDtos.AccountProfileResponse register(AccountDtos.RegisterRequest request) {
        guard.requireEnabled();
        UserAccount account = users.registerCustomer(
                request.email(), request.displayName(), request.password(), request.phone());
        return users.profile(account.getEmail());
    }

    @Transactional(readOnly = true)
    public AccountDtos.Overview overview(String email) {
        guard.requireEnabled();
        ApiDtos.AccountProfileResponse profile = users.profile(email);
        AccountSectionProvider.AccountContext context =
                new AccountSectionProvider.AccountContext(
                        profile.id(), profile.email(), profile.displayName());
        List<AccountSectionProvider.AccountSection> sections = new ArrayList<>();
        for (AccountSectionProvider provider : providers) {
            if (!plugins.isEnabled(provider.pluginId())) continue;
            try {
                sections.add(provider.load(context));
            } catch (RuntimeException exception) {
                sections.add(new AccountSectionProvider.AccountSection(
                        provider.key(), provider.pluginId(), provider.label(),
                        provider.icon(), provider.order(), "ERROR",
                        "La integración no está disponible temporalmente.",
                        List.of(), Map.of("error", exception.getMessage() == null ? exception.getClass().getSimpleName() : exception.getMessage())));
            }
        }
        return new AccountDtos.Overview(profile, List.copyOf(sections));
    }

    @Transactional(readOnly = true)
    public AccountDtos.AdminStatus adminStatus() {
        List<AccountDtos.IntegrationStatus> integrations = providers.stream()
                .map(provider -> {
                    boolean enabled = plugins.isEnabled(provider.pluginId());
                    return new AccountDtos.IntegrationStatus(
                            provider.key(), provider.pluginId(), provider.label(),
                            provider.icon(), enabled, enabled ? "CONNECTED" : "PLUGIN_DISABLED");
                })
                .toList();
        return new AccountDtos.AdminStatus(
                plugins.isEnabled("trastaccount"),
                integrations.size(),
                integrations);
    }
}

package com.nattechnologies.trastcms.plugins.account;

import com.nattechnologies.trastcms.plugin.api.AccountSectionProvider;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import jakarta.validation.constraints.*;
import java.util.List;

public final class AccountDtos {
    private AccountDtos() { }

    public record RegisterRequest(
            @Email @NotBlank @Size(max = 190) String email,
            @NotBlank @Size(max = 120) String displayName,
            @NotBlank @Size(min = 12, max = 200) String password,
            @Size(max = 60) String phone) { }

    public record Overview(
            ApiDtos.AccountProfileResponse profile,
            List<AccountSectionProvider.AccountSection> sections) { }

    public record IntegrationStatus(
            String key, String pluginId, String label, String icon,
            boolean pluginEnabled, String status) { }

    public record AdminStatus(
            boolean enabled,
            int integrationCount,
            List<IntegrationStatus> integrations) { }
}

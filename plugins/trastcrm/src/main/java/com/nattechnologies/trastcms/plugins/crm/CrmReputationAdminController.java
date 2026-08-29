package com.nattechnologies.trastcms.plugins.crm;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/crm/reputation")
public class CrmReputationAdminController {
    private final CrmReputationService reputation;
    private final CrmPluginGuard guard;

    public CrmReputationAdminController(CrmReputationService reputation, CrmPluginGuard guard) {
        this.reputation = reputation;
        this.guard = guard;
    }

    @ModelAttribute void requireEnabled() { guard.requireEnabled(); }

    @GetMapping("/summary")
    public CrmReputationDtos.Summary summary() { return reputation.summary(); }

    @GetMapping("/requests")
    public CrmDtos.PageResponse<CrmReputationDtos.RequestResponse> requests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return reputation.list(page, size);
    }

    @GetMapping("/settings")
    public CrmReputationDtos.Settings settings() { return reputation.settings(); }

    @PutMapping("/settings")
    public CrmReputationDtos.Settings saveSettings(@Valid @RequestBody CrmReputationDtos.Settings request) {
        return reputation.saveSettings(request);
    }

    @PostMapping("/requests/{id}/resend")
    public CrmReputationDtos.RequestResponse resend(@PathVariable String id) {
        return reputation.resend(id);
    }
}

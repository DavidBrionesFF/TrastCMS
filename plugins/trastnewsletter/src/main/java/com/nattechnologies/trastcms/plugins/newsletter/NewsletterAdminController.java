package com.nattechnologies.trastcms.plugins.newsletter;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/newsletter")
public class NewsletterAdminController {

    private final NewsletterService service;
    private final NewsletterPluginGuard guard;

    public NewsletterAdminController(NewsletterService service, NewsletterPluginGuard guard) {
        this.service = service;
        this.guard = guard;
    }

    @ModelAttribute
    void requireEnabled() {
        guard.requireEnabled();
    }

    @GetMapping("/stats")
    public NewsletterDtos.NewsletterStats stats() {
        return service.stats();
    }

    @GetMapping("/subscribers")
    public Page<NewsletterDtos.SubscriberResponse> subscribers(@RequestParam(required = false) String status,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "25") int size) {
        return service.listSubscribers(status, page, size);
    }

    @DeleteMapping("/subscribers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubscriber(@PathVariable String id) {
        service.deleteSubscriber(id);
    }

    @GetMapping("/campaigns")
    public Page<NewsletterDtos.CampaignResponse> campaigns(@RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "25") int size) {
        return service.listCampaigns(page, size);
    }

    @PostMapping("/campaigns")
    @ResponseStatus(HttpStatus.CREATED)
    public NewsletterDtos.CampaignResponse createCampaign(@Valid @RequestBody NewsletterDtos.CampaignRequest request) {
        return service.saveCampaign(null, request, "Admin");
    }

    @PutMapping("/campaigns/{id}")
    public NewsletterDtos.CampaignResponse updateCampaign(@PathVariable String id, @Valid @RequestBody NewsletterDtos.CampaignRequest request) {
        return service.saveCampaign(id, request, "Admin");
    }

    @PostMapping("/campaigns/{id}/send")
    public void sendCampaign(@PathVariable String id, @RequestBody NewsletterDtos.SendCampaignRequest request) {
        service.sendCampaign(id, request.includeEmail(), request.includePush());
    }

    @GetMapping("/settings")
    public NewsletterDtos.NewsletterSettingsResponse getSettings() {
        return service.getSettings();
    }

    @PutMapping("/settings")
    public void saveSettings(@Valid @RequestBody NewsletterDtos.NewsletterSettingsRequest request) {
        service.saveSettings(request);
    }
}

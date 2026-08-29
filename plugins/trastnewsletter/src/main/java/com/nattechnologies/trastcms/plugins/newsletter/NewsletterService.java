package com.nattechnologies.trastcms.plugins.newsletter;

import com.nattechnologies.trastcms.domain.push.PushSubscription;
import com.nattechnologies.trastcms.service.PlatformMailService;
import com.nattechnologies.trastcms.service.SiteSettingService;
import com.nattechnologies.trastcms.service.WebPushService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service

public class NewsletterService {

    private final NewsletterSubscriberRepository subscriberRepo;
    private final NewsletterCampaignRepository campaignRepo;
    private final NewsletterSendLogRepository logRepo;
    private final SiteSettingService settingsService;
    private final PlatformMailService mailService;
    private final WebPushService webPushService;

    public NewsletterService(NewsletterSubscriberRepository subscriberRepo,
                             NewsletterCampaignRepository campaignRepo,
                             NewsletterSendLogRepository logRepo,
                             SiteSettingService settingsService,
                             PlatformMailService mailService,
                             WebPushService webPushService) {
        this.subscriberRepo = subscriberRepo;
        this.campaignRepo = campaignRepo;
        this.logRepo = logRepo;
        this.settingsService = settingsService;
        this.mailService = mailService;
        this.webPushService = webPushService;
    }

    @Transactional
    public NewsletterDtos.SubscriberResponse subscribe(NewsletterDtos.SubscribeRequest req, String ipAddress) {
        String email = req.email().trim().toLowerCase();
        NewsletterSubscriber subscriber = subscriberRepo.findByEmail(email).orElse(null);

        if (subscriber == null) {
            subscriber = new NewsletterSubscriber();
            subscriber.setEmail(email);
            subscriber.setName(req.name());
            subscriber.setToken(UUID.randomUUID().toString().replace("-", ""));
            subscriber.setSource("widget");
            subscriber.setStatus("ACTIVE");
        } else {
            subscriber.setStatus("ACTIVE");
            if (req.name() != null) subscriber.setName(req.name());
        }

        subscriber = subscriberRepo.save(subscriber);

        // Notificar email de bienvenida
        try {
            mailService.send(email, "¡Bienvenido a nuestro Boletín!", "Gracias por suscribirte a nuestras novedades.");
        } catch (Exception ignored) {}

        return toSubscriberDto(subscriber);
    }

    @Transactional
    public void unsubscribeByToken(String token) {
        subscriberRepo.findByToken(token).ifPresent(s -> {
            s.setStatus("UNSUBSCRIBED");
            s.setUnsubscribedAt(Instant.now());
            subscriberRepo.save(s);
        });
    }

    public Page<NewsletterDtos.SubscriberResponse> listSubscribers(String status, int page, int size) {
        PageRequest pr = PageRequest.of(page, size);
        if (status != null && !status.isBlank()) {
            return subscriberRepo.findByStatus(status, pr).map(this::toSubscriberDto);
        }
        return subscriberRepo.findAll(pr).map(this::toSubscriberDto);
    }

    @Transactional
    public void deleteSubscriber(String id) {
        subscriberRepo.deleteById(id);
    }

    public NewsletterDtos.NewsletterStats stats() {
        long totalSub = subscriberRepo.count();
        long activeSub = subscriberRepo.countByStatus("ACTIVE");
        long totalCamp = campaignRepo.count();
        long sentCamp = campaignRepo.findAll().stream().filter(c -> "SENT".equals(c.getStatus())).count();
        return new NewsletterDtos.NewsletterStats(totalSub, activeSub, totalCamp, sentCamp);
    }

    public Page<NewsletterDtos.CampaignResponse> listCampaigns(int page, int size) {
        return campaignRepo.findAll(PageRequest.of(page, size)).map(this::toCampaignDto);
    }

    @Transactional
    public NewsletterDtos.CampaignResponse saveCampaign(String id, NewsletterDtos.CampaignRequest req, String createdBy) {
        NewsletterCampaign campaign;
        if (id != null) {
            campaign = campaignRepo.findById(id).orElseThrow();
        } else {
            campaign = new NewsletterCampaign();
            campaign.setCreatedBy(createdBy);
        }

        campaign.setTitle(req.title());
        campaign.setSubject(req.subject());
        campaign.setBodyHtml(req.bodyHtml());
        campaign.setPushEnabled(req.pushEnabled());
        campaign.setPushTitle(req.pushTitle());
        campaign.setPushBody(req.pushBody());
        campaign.setPushUrl(req.pushUrl());

        return toCampaignDto(campaignRepo.save(campaign));
    }

    @Transactional
    public void sendCampaign(String campaignId, boolean includeEmail, boolean includePush) {
        NewsletterCampaign campaign = campaignRepo.findById(campaignId).orElseThrow();
        List<NewsletterSubscriber> activeSubscribers = subscriberRepo.findAll().stream()
                .filter(s -> "ACTIVE".equals(s.getStatus()))
                .toList();

        if (includeEmail) {
            for (NewsletterSubscriber s : activeSubscribers) {
                try {
                    String body = campaign.getBodyHtml() + "<br/><hr/><p><small><a href=\"/api/public/newsletter/unsubscribe?token=" + s.getToken() + "\">Desuscribirme</a></small></p>";
                    mailService.send(s.getEmail(), campaign.getSubject(), body);

                    NewsletterSendLog log = new NewsletterSendLog();
                    log.setCampaignId(campaign.getId());
                    log.setSubscriberId(s.getId());
                    log.setStatus("SENT");
                    logRepo.save(log);
                } catch (Exception ignored) {}
            }
        }

        if (includePush && campaign.isPushEnabled()) {
            webPushService.sendToAll(
                    campaign.getPushTitle() != null ? campaign.getPushTitle() : campaign.getTitle(),
                    campaign.getPushBody() != null ? campaign.getPushBody() : campaign.getSubject(),
                    campaign.getPushUrl() != null ? campaign.getPushUrl() : "/",
                    null
            );
        }

        campaign.setStatus("SENT");
        campaign.setSentAt(Instant.now());
        campaign.setRecipientCount(activeSubscribers.size());
        campaignRepo.save(campaign);
    }

    public NewsletterDtos.NewsletterSettingsResponse getSettings() {
        return new NewsletterDtos.NewsletterSettingsResponse(
                settingsService.get("newsletter.sender_name", "TrastCMS"),
                settingsService.get("newsletter.sender_email", "noreply@trastcms.com"),
                settingsService.get("newsletter.reply_to", ""),
                Boolean.parseBoolean(settingsService.get("newsletter.double_opt_in", "false")),
                settingsService.get("newsletter.logo_url", ""),
                settingsService.get("newsletter.footer_text", "TrastCMS Newsletter")
        );
    }

    @Transactional
    public void saveSettings(NewsletterDtos.NewsletterSettingsRequest req) {
        settingsService.put("newsletter.sender_name", req.senderName());
        settingsService.put("newsletter.sender_email", req.senderEmail());
        settingsService.put("newsletter.reply_to", req.replyTo());
        settingsService.put("newsletter.double_opt_in", String.valueOf(req.doubleOptIn()));
        settingsService.put("newsletter.logo_url", req.logoUrl());
        settingsService.put("newsletter.footer_text", req.footerText());
    }

    private NewsletterDtos.SubscriberResponse toSubscriberDto(NewsletterSubscriber s) {
        return new NewsletterDtos.SubscriberResponse(s.getId(), s.getEmail(), s.getName(), s.getStatus(), s.getSource(), s.getSubscribedAt());
    }

    private NewsletterDtos.CampaignResponse toCampaignDto(NewsletterCampaign c) {
        return new NewsletterDtos.CampaignResponse(c.getId(), c.getTitle(), c.getSubject(), c.getBodyHtml(),
                c.isPushEnabled(), c.getPushTitle(), c.getPushBody(), c.getPushUrl(), c.getStatus(),
                c.getScheduledAt(), c.getSentAt(), c.getRecipientCount(), c.getOpenCount(), c.getCreatedBy(),
                c.getCreatedAt(), c.getUpdatedAt());
    }
}

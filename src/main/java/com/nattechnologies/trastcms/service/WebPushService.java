package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.domain.push.PushSubscription;
import com.nattechnologies.trastcms.domain.push.PushSubscriptionRepository;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
public class WebPushService {

    private final PushSubscriptionRepository repository;
    private final SiteSettingService siteSettingService;
    private final HttpClient httpClient;

    public WebPushService(PushSubscriptionRepository repository, SiteSettingService siteSettingService) {
        this.repository = repository;
        this.siteSettingService = siteSettingService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public String getPublicVapidKey() {
        String key = siteSettingService.get("push.vapid.public_key", null);
        if (key == null || key.isBlank()) {
            key = Base64.getUrlEncoder().withoutPadding().encodeToString(UUID.randomUUID().toString().getBytes());
            siteSettingService.put("push.vapid.public_key", key);
            siteSettingService.put("push.vapid.private_key", Base64.getUrlEncoder().withoutPadding().encodeToString(UUID.randomUUID().toString().getBytes()));
        }
        return key;
    }

    public PushSubscription subscribe(String endpoint, String p256dhKey, String authKey, String userAgent, String email) {
        PushSubscription sub = repository.findByEndpoint(endpoint).orElse(new PushSubscription());
        sub.setEndpoint(endpoint);
        sub.setP256dhKey(p256dhKey);
        sub.setAuthKey(authKey);
        sub.setUserAgent(userAgent);
        sub.setSubscriberEmail(email);
        sub.setActive(true);
        return repository.save(sub);
    }

    public void sendToAll(String title, String body, String url, String icon) {
        List<PushSubscription> activeSubs = repository.findByActiveTrue();
        String jsonPayload = String.format("{\"title\":\"%s\",\"body\":\"%s\",\"url\":\"%s\",\"icon\":\"%s\"}", title, body, url, icon != null ? icon : "/icon-192.png");

        for (PushSubscription sub : activeSubs) {
            try {
                HttpRequest req = HttpRequest.newBuilder()
                        .uri(URI.create(sub.getEndpoint()))
                        .header("Content-Type", "application/json")
                        .header("TTL", "86400")
                        .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                        .build();

                httpClient.sendAsync(req, HttpResponse.BodyHandlers.discarding());
            } catch (Exception ignored) {}
        }
    }
}

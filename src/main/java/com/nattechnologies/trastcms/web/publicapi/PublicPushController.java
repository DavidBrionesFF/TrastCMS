package com.nattechnologies.trastcms.web.publicapi;

import com.nattechnologies.trastcms.domain.push.PushDtos;
import com.nattechnologies.trastcms.service.WebPushService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/public/push")
public class PublicPushController {

    private final WebPushService pushService;

    public PublicPushController(WebPushService pushService) {
        this.pushService = pushService;
    }

    @GetMapping("/vapid-key")
    public Map<String, String> getVapidKey() {
        return Map.of("publicKey", pushService.getPublicVapidKey());
    }

    @PostMapping("/subscribe")
    public Map<String, String> subscribe(@RequestBody PushDtos.SubscribeRequest req) {
        pushService.subscribe(req.endpoint(), req.p256dhKey(), req.authKey(), req.userAgent(), null);
        return Map.of("status", "subscribed");
    }
}

package com.nattechnologies.trastcms.web.admin;

import com.nattechnologies.trastcms.domain.push.PushDtos;
import com.nattechnologies.trastcms.service.WebPushService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/push")
public class AdminPushController {

    private final WebPushService pushService;

    public AdminPushController(WebPushService pushService) {
        this.pushService = pushService;
    }

    @PostMapping("/test")
    public Map<String, String> testPush(@RequestBody PushDtos.SendTestRequest req) {
        pushService.sendToAll(req.title(), req.body(), req.url(), null);
        return Map.of("status", "sent");
    }
}

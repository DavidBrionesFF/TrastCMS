package com.nattechnologies.trastcms.plugins.newsletter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/newsletter")
public class NewsletterPublicController {

    private final NewsletterService service;

    public NewsletterPublicController(NewsletterService service) {
        this.service = service;
    }

    @PostMapping("/subscribe")
    public NewsletterDtos.SubscriberResponse subscribe(@Valid @RequestBody NewsletterDtos.SubscribeRequest request, HttpServletRequest req) {
        return service.subscribe(request, req.getRemoteAddr());
    }

    @GetMapping("/unsubscribe")
    public String unsubscribe(@RequestParam String token) {
        service.unsubscribeByToken(token);
        return "Suscripción cancelada correctamente.";
    }
}

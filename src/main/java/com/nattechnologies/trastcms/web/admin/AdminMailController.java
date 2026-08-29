package com.nattechnologies.trastcms.web.admin;

import com.nattechnologies.trastcms.service.MailSettingsService;
import com.nattechnologies.trastcms.service.PlatformMailService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/mail")
public class AdminMailController {
    private final MailSettingsService settings;
    private final PlatformMailService mail;

    public AdminMailController(
            MailSettingsService settings,
            PlatformMailService mail) {
        this.settings = settings;
        this.mail = mail;
    }

    @GetMapping("/settings")
    public MailSettingsService.MailSettingsResponse settings() {
        return settings.get();
    }

    @PutMapping("/settings")
    public MailSettingsService.MailSettingsResponse update(
            @Valid @RequestBody MailSettingsService.MailSettingsRequest request) {
        return settings.update(request);
    }

    @PostMapping("/test")
    public PlatformMailService.MailResult test(
            @Valid @RequestBody TestMailRequest request) {
        return mail.test(request.recipient());
    }

    public record TestMailRequest(@Email @NotBlank String recipient) { }
}

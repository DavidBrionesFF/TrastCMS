package com.nattechnologies.trastcms.plugins.translate;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/translation")
class TranslationPublicController {
    private final TranslationService service;

    TranslationPublicController(TranslationService service) {
        this.service = service;
    }

    @GetMapping("/settings")
    TranslationDtos.SettingsResponse settings() {
        return service.publicSettings();
    }
}

@RestController
@RequestMapping("/api/admin/translation")
class TranslationAdminController {
    private final TranslationService service;

    TranslationAdminController(TranslationService service) {
        this.service = service;
    }

    @GetMapping("/settings")
    TranslationDtos.SettingsResponse settings() {
        return service.settingsResponse();
    }

    @PutMapping("/settings")
    TranslationDtos.SettingsResponse save(@Valid @RequestBody TranslationDtos.SettingsRequest request) {
        return service.save(request);
    }
}

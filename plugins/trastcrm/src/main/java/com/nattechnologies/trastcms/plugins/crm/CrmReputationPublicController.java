package com.nattechnologies.trastcms.plugins.crm;

import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/public/crm/reputation")
public class CrmReputationPublicController {
    private final CrmReputationService reputation;

    public CrmReputationPublicController(CrmReputationService reputation) {
        this.reputation = reputation;
    }

    @GetMapping("/{token}")
    public CrmReputationDtos.PublicView view(@PathVariable String token) {
        return reputation.publicView(token);
    }

    @PostMapping("/{token}/rating")
    public CrmReputationDtos.PublicView rating(
            @PathVariable String token,
            @Valid @RequestBody CrmReputationDtos.RatingRequest request) {
        return reputation.rate(token, request.rating());
    }

    @PostMapping("/{token}/feedback")
    public CrmReputationDtos.PublicView feedback(
            @PathVariable String token,
            @Valid @RequestBody CrmReputationDtos.FeedbackRequest request) {
        return reputation.feedback(token, request.feedback());
    }

    @GetMapping("/{token}/channels/{channelId}")
    public ResponseEntity<Void> channel(@PathVariable String token, @PathVariable String channelId) {
        URI target = reputation.channelRedirect(token, channelId);
        return ResponseEntity.status(HttpStatus.FOUND).header(HttpHeaders.LOCATION, target.toString()).build();
    }
}

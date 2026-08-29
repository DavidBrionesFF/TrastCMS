package com.nattechnologies.trastcms.plugins.comments;

import com.nattechnologies.trastcms.domain.plugin.BundledPluginStateRepository;
import com.nattechnologies.trastcms.service.NotFoundException;
import org.springframework.stereotype.Component;

@Component
public class CommentsPluginGuard {

    private final BundledPluginStateRepository stateRepo;

    public CommentsPluginGuard(BundledPluginStateRepository stateRepo) {
        this.stateRepo = stateRepo;
    }

    public void requireEnabled() {
        var state = stateRepo.findCurrent("trastcomments");
        if (state.isEmpty() || !state.get().isEnabled()) {
            throw new NotFoundException("Comments plugin is not enabled");
        }
    }
}

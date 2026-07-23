package com.nattechnologies.trastcms.config;

import com.nattechnologies.trastcms.domain.category.Category;
import com.nattechnologies.trastcms.domain.media.MediaAsset;
import com.nattechnologies.trastcms.domain.plugin.PluginRegistration;
import com.nattechnologies.trastcms.domain.post.Post;
import com.nattechnologies.trastcms.domain.post.PostRevision;
import com.nattechnologies.trastcms.domain.setting.SiteSetting;
import com.nattechnologies.trastcms.domain.user.UserAccount;
import com.nattechnologies.trastcms.plugin.api.PluginEvent;
import com.nattechnologies.trastcms.service.ThemeService;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.context.annotation.Configuration;

@Configuration
@RegisterReflectionForBinding({
    UserAccount.class, Category.class, Post.class, PostRevision.class,
    MediaAsset.class, SiteSetting.class, PluginRegistration.class,
    ThemeService.ThemeManifest.class, PluginEvent.class
})
public class NativeHints {}

package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.domain.category.CategoryRepository;
import com.nattechnologies.trastcms.domain.media.MediaAssetRepository;
import com.nattechnologies.trastcms.domain.post.ContentType;
import com.nattechnologies.trastcms.domain.post.PostRepository;
import com.nattechnologies.trastcms.domain.post.PostStatus;
import com.nattechnologies.trastcms.domain.site.CmsSiteMembershipRepository;
import com.nattechnologies.trastcms.domain.user.UserRole;
import com.nattechnologies.trastcms.multisite.SiteAccessService;
import com.nattechnologies.trastcms.multisite.SiteContext;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final PostRepository posts;
    private final CategoryRepository categories;
    private final MediaAssetRepository media;
    private final CmsSiteMembershipRepository memberships;
    private final SiteAccessService access;

    public DashboardService(PostRepository posts, CategoryRepository categories, MediaAssetRepository media,
                            CmsSiteMembershipRepository memberships, SiteAccessService access) {
        this.posts = posts;
        this.categories = categories;
        this.media = media;
        this.memberships = memberships;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public ApiDtos.Dashboard dashboard(String actorEmail) {
        UserRole role = access.role(actorEmail, SiteContext.siteId())
                .orElseThrow(() -> new NotFoundException("El usuario no pertenece a este sitio"));
        if (role == UserRole.AUTHOR) {
            return new ApiDtos.Dashboard(
                    posts.countByAuthorEmailIgnoreCase(actorEmail), 0,
                    posts.countByAuthorEmailIgnoreCaseAndStatus(actorEmail, PostStatus.PUBLISHED),
                    posts.countByAuthorEmailIgnoreCaseAndStatus(actorEmail, PostStatus.DRAFT), 0, 0, 0);
        }
        long userCount = role == UserRole.ADMIN ? memberships.countBySiteIdAndEnabledTrue(SiteContext.siteId()) : 0;
        return new ApiDtos.Dashboard(
                posts.countByContentType(ContentType.POST), posts.countByContentType(ContentType.PAGE),
                posts.countByStatus(PostStatus.PUBLISHED), posts.countByStatus(PostStatus.DRAFT),
                categories.count(), media.count(), userCount);
    }
}

package com.nattechnologies.trastcms.service;

import com.nattechnologies.trastcms.domain.category.CategoryRepository;
import com.nattechnologies.trastcms.domain.media.MediaAssetRepository;
import com.nattechnologies.trastcms.domain.post.ContentType;
import com.nattechnologies.trastcms.domain.post.PostRepository;
import com.nattechnologies.trastcms.domain.post.PostStatus;
import com.nattechnologies.trastcms.domain.user.UserAccount;
import com.nattechnologies.trastcms.domain.user.UserAccountRepository;
import com.nattechnologies.trastcms.domain.user.UserRole;
import com.nattechnologies.trastcms.web.dto.ApiDtos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private final PostRepository posts;
    private final CategoryRepository categories;
    private final MediaAssetRepository media;
    private final UserAccountRepository users;

    public DashboardService(
            PostRepository posts,
            CategoryRepository categories,
            MediaAssetRepository media,
            UserAccountRepository users) {
        this.posts = posts;
        this.categories = categories;
        this.media = media;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public ApiDtos.Dashboard dashboard(String actorEmail) {
        UserAccount actor = users.findByEmailIgnoreCase(actorEmail)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        if (actor.getRole() == UserRole.AUTHOR) {
            return new ApiDtos.Dashboard(
                    posts.countByAuthorEmailIgnoreCase(actorEmail),
                    0,
                    posts.countByAuthorEmailIgnoreCaseAndStatus(
                            actorEmail, PostStatus.PUBLISHED),
                    posts.countByAuthorEmailIgnoreCaseAndStatus(
                            actorEmail, PostStatus.DRAFT),
                    0,
                    0,
                    0);
        }

        long userCount = actor.getRole() == UserRole.ADMIN
                ? users.count()
                : 0;

        return new ApiDtos.Dashboard(
                posts.countByContentType(ContentType.POST),
                posts.countByContentType(ContentType.PAGE),
                posts.countByStatus(PostStatus.PUBLISHED),
                posts.countByStatus(PostStatus.DRAFT),
                categories.count(),
                media.count(),
                userCount);
    }
}

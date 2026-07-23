package com.nattechnologies.trastcms.plugin.api;

import com.nattechnologies.trastcms.domain.post.Post;

/**
 * Punto de extensión compilado con el núcleo. En Native Image, las implementaciones
 * deben estar presentes durante la compilación. Los plugins instalables después del
 * despliegue usan los webhooks externos.
 */
public interface ContentHook {
    default int order() { return 100; }
    default void beforeSave(Post post, HookContext context) {}
    default void afterPublish(Post post, HookContext context) {}
}

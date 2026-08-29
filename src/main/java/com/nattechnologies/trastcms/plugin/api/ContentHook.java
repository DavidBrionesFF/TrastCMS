package com.nattechnologies.trastcms.plugin.api;

import com.nattechnologies.trastcms.domain.post.Post;

/**
 * Hook de contenido compilado con el núcleo.
 *
 * <p>Permite modificar el contenido antes de guardarlo y después de publicarlo.
 * En Native Image, las implementaciones deben estar presentes durante la compilación.
 * Los plugins instalables después del despliegue usan los webhooks externos.</p>
 */
public interface ContentHook {
    default int order() { return 100; }
    default void beforeSave(Post post, HookContext context) {}
    default void afterPublish(Post post, HookContext context) {}
}

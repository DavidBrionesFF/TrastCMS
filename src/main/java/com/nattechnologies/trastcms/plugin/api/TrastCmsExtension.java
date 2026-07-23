package com.nattechnologies.trastcms.plugin.api;

import org.pf4j.ExtensionPoint;

import java.util.List;
import java.util.Map;

/**
 * Stable extension contract exposed to trusted JVM plugins.
 *
 * <p>Plugins can contribute visual-builder blocks, declarative administration
 * pages, menu entries and event listeners. Actions are invoked through the
 * authenticated TrastCMS administration API, so plugins do not need to
 * register Spring MVC controllers dynamically.</p>
 */
public interface TrastCmsExtension extends ExtensionPoint {
    default String name() { return getClass().getSimpleName(); }
    default String description() { return ""; }
    default List<String> capabilities() { return List.of(); }

    /** Declarative blocks consumed by the visual page builder. */
    default List<Map<String, Object>> blocks() { return List.of(); }

    /**
     * Declarative administration pages. Each page may contain an id, label,
     * icon, description and a list of safe UI sections (notice, stats, links
     * and form). TrastCMS renders the page in its own Vue shell.
     */
    default List<Map<String, Object>> adminMenuItems() { return List.of(); }

    /** Action identifiers accepted by {@link #handleAdminAction}. */
    default List<String> adminActions() { return List.of(); }

    /**
     * Executes an authenticated action requested by a declarative plugin page.
     * Plugins should return JSON-compatible values only.
     */
    default Map<String, Object> handleAdminAction(
            String action,
            Map<String, Object> input) {
        throw new UnsupportedOperationException(
                "La extensión no implementa la acción " + action);
    }

    default void onEvent(PluginEvent event) { }
}

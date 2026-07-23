package com.nattechnologies.trastcms.plugin.api;

import java.util.List;
import java.util.Map;

/**
 * First-party extension compiled into the single TrastCMS JAR. Bundled plugins
 * keep their source code isolated under plugins/, can be enabled or disabled at
 * runtime and remain compatible with GraalVM Native Image.
 */
public interface BundledPlugin {
    String id();
    String name();
    String version();
    default String description() { return ""; }
    default String author() { return "NaT Technologies"; }
    default boolean enabledByDefault() { return false; }
    default List<String> capabilities() { return List.of(); }
    default List<Map<String, Object>> blocks() { return List.of(); }
    default List<Map<String, Object>> adminMenuItems() { return List.of(); }
    default List<String> adminActions() { return List.of(); }
    default Map<String, Object> handleAdminAction(String action, Map<String, Object> input) {
        throw new UnsupportedOperationException("Acción no implementada: " + action);
    }
    default void onEnabled() { }
    default void onDisabled() { }
    default void onEvent(PluginEvent event) { }
}

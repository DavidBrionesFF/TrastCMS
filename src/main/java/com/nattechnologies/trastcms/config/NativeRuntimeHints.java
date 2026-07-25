package com.nattechnologies.trastcms.config;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;

/**
 * Recursos que TrastCMS carga dinámicamente desde el classpath y que deben
 * conservarse cuando Spring AOT construye un ejecutable nativo.
 */
public class NativeRuntimeHints implements RuntimeHintsRegistrar {
    @Override
    public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
        hints.resources().registerPattern("static/**");
        hints.resources().registerPattern("themes/**");
        hints.resources().registerPattern("db/migration/**");
        hints.resources().registerPattern("META-INF/services/**");
        hints.resources().registerPattern("application*.yml");
        hints.resources().registerPattern("application*.properties");
    }
}

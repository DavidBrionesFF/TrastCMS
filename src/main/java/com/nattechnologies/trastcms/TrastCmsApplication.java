package com.nattechnologies.trastcms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.context.annotation.ImportRuntimeHints;
import com.nattechnologies.trastcms.config.NativeRuntimeHints;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Punto de entrada de TrastCMS.
 *
 * <p>Prepara el directorio de datos, habilita la ejecución asíncrona
 * y registra las hints de Native Image antes de iniciar Spring Boot.</p>
 */
@EnableAsync
@org.springframework.cache.annotation.EnableCaching
@ImportRuntimeHints(NativeRuntimeHints.class)
@SpringBootApplication
public class TrastCmsApplication {
    /**
     * Inicia la aplicación. Crea los directorios {@code data/db} y
     * {@code data/uploads} si no existen y luego arranca el contexto Spring.
     *
     * @param args argumentos de línea de comandos
     */
    public static void main(String[] args) {
        loadDotEnv();
        String dataDir = System.getenv().getOrDefault("TRASTCMS_DATA_DIR", System.getProperty("TRASTCMS_DATA_DIR", "./data"));
        try {
            Files.createDirectories(Path.of(dataDir));
            Files.createDirectories(Path.of(dataDir, "db"));
            Files.createDirectories(Path.of(dataDir, "uploads"));
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo preparar el directorio de datos: " + dataDir, exception);
        }
        SpringApplication.run(TrastCmsApplication.class, args);
    }

    private static void loadDotEnv() {
        Path envFile = Path.of(".env");
        if (Files.exists(envFile)) {
            try {
                for (String line : Files.readAllLines(envFile)) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                        int idx = trimmed.indexOf('=');
                        String key = trimmed.substring(0, idx).trim();
                        String val = trimmed.substring(idx + 1).trim();
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, val);
                        }
                    }
                }
            } catch (Exception ignored) { }
        }
    }
}

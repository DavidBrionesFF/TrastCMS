package com.nattechnologies.trastcms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.nio.file.Files;
import java.nio.file.Path;

@EnableAsync
@SpringBootApplication
public class TrastCmsApplication {
    public static void main(String[] args) {
        String dataDir = System.getenv().getOrDefault("TRASTCMS_DATA_DIR", "./data");
        try {
            Files.createDirectories(Path.of(dataDir));
            Files.createDirectories(Path.of(dataDir, "db"));
            Files.createDirectories(Path.of(dataDir, "uploads"));
        } catch (Exception exception) {
            throw new IllegalStateException("No se pudo preparar el directorio de datos: " + dataDir, exception);
        }
        SpringApplication.run(TrastCmsApplication.class, args);
    }
}

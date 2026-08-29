package com.nattechnologies.trastcms.plugins.maintenance;

import com.nattechnologies.trastcms.service.SiteSettingService;

import org.springframework.cache.CacheManager;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class MaintenanceService {

    private final MaintBackupJobRepository backupRepository;
    private final DataSource dataSource;
    private final CacheManager cacheManager;
    private final SiteSettingService settings;

    public MaintenanceService(MaintBackupJobRepository backupRepository, DataSource dataSource,
                              org.springframework.beans.factory.ObjectProvider<CacheManager> cacheManagerProvider,
                              SiteSettingService settings) {
        this.backupRepository = backupRepository;
        this.dataSource = dataSource;
        this.cacheManager = cacheManagerProvider.getIfAvailable();
        this.settings = settings;
    }

    public MaintenanceDtos.BackupJobResponse triggerBackup(String type, String triggeredBy) {
        String id = UUID.randomUUID().toString();
        MaintBackupJob job = new MaintBackupJob(id, type, "PENDING", triggeredBy, Instant.now());
        backupRepository.save(job);

        CompletableFuture.runAsync(() -> {
            try {
                job.setStatus("RUNNING");
                job.setStartedAt(Instant.now());
                backupRepository.save(job);

                String backupFileName = "backup_" + type.toLowerCase() + "_" + id + ".sql";
                Path tempDir = Paths.get(System.getProperty("java.io.tmpdir"), "trastcms_backups");
                Files.createDirectories(tempDir);
                Path backupPath = tempDir.resolve(backupFileName);

                if ("DATABASE".equals(type) || "FULL".equals(type)) {
                    try (Connection conn = dataSource.getConnection();
                         Statement stmt = conn.createStatement()) {
                        String dbUrl = conn.getMetaData().getURL();
                        if (dbUrl.contains("h2")) {
                            stmt.execute("SCRIPT TO '" + backupPath.toAbsolutePath().toString() + "'");
                        } else if (dbUrl.contains("postgresql")) {
                            // Dummy fallback for PG
                            Files.writeString(backupPath, "-- Postgres backup not fully implemented in demo");
                        }
                    }
                } else if ("FILES".equals(type)) {
                    Files.writeString(backupPath, "Dummy files backup for: " + id);
                }

                job.setFilePath(backupPath.toAbsolutePath().toString());
                File f = backupPath.toFile();
                if (f.exists()) {
                    job.setFileSizeBytes(f.length());
                }
                job.setStatus("DONE");
                job.setFinishedAt(Instant.now());
                backupRepository.save(job);
            } catch (Exception e) {
                job.setStatus("FAILED");
                job.setNotes(e.getMessage());
                job.setFinishedAt(Instant.now());
                backupRepository.save(job);
            }
        });

        return toDto(job);
    }

    public Page<MaintenanceDtos.BackupJobResponse> listBackups(int page, int size) {
        return backupRepository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")))
                .map(this::toDto);
    }

    @Transactional
    public void deleteBackup(String id) {
        backupRepository.findById(id).ifPresent(job -> {
            if (job.getFilePath() != null) {
                File f = new File(job.getFilePath());
                if (f.exists()) {
                    f.delete();
                }
            }
            backupRepository.delete(job);
        });
    }

    public Resource downloadBackup(String id) {
        return backupRepository.findById(id)
            .map(job -> new FileSystemResource(job.getFilePath()))
            .orElseThrow(() -> new RuntimeException("Backup no encontrado"));
    }

    public void uploadToGoogleDrive(String backupId) {
        MaintBackupJob job = backupRepository.findById(backupId)
                .orElseThrow(() -> new RuntimeException("Backup not found"));

        if (job.getFilePath() == null || !new File(job.getFilePath()).exists()) {
            throw new RuntimeException("Backup file not found on disk");
        }

        if (!settings.getBoolean("maintenance.google_drive_enabled", false)) {
            throw new RuntimeException("Google Drive no está habilitado para este sitio");
        }

        // Upload provider wiring remains network infrastructure, while whether a
        // site may use it and its destination folder are tenant-scoped settings.
        CompletableFuture.runAsync(() -> {
            try {
                // Dummy logic for Drive upload
                Thread.sleep(1000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public MaintenanceDtos.CacheStatsResponse getCacheStats() {
        List<MaintenanceDtos.CacheInfo> list = new ArrayList<>();
        if (cacheManager != null) {
            for (String name : cacheManager.getCacheNames()) {
                list.add(new MaintenanceDtos.CacheInfo(name, 0, "Cache for " + name));
            }
        }
        return new MaintenanceDtos.CacheStatsResponse(list);
    }

    public void evictAllCaches() {
        if (cacheManager != null) {
            for (String name : cacheManager.getCacheNames()) {
                org.springframework.cache.Cache cache = cacheManager.getCache(name);
                if (cache != null) cache.clear();
            }
        }
    }

    public MaintenanceDtos.HealthResponse getSystemHealth() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        MemoryMXBean memBean = ManagementFactory.getMemoryMXBean();
        RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();

        double cpu = osBean.getSystemLoadAverage();
        long heapUsed = memBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long heapMax = memBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
        long uptime = runtimeBean.getUptime() / 1000;

        boolean dbConnected = false;
        try (Connection conn = dataSource.getConnection()) {
            dbConnected = conn.isValid(2);
        } catch (Exception e) {
            dbConnected = false;
        }

        return new MaintenanceDtos.HealthResponse(cpu, heapUsed, heapMax, uptime, dbConnected, System.getProperty("java.version"), osBean.getName());
    }

    public MaintenanceDtos.MaintenanceSettingsResponse getSettings() {
        return new MaintenanceDtos.MaintenanceSettingsResponse(
                settings.getBoolean("maintenance.google_drive_enabled", false),
                settings.get("maintenance.google_drive_folder", "Backups"),
                settings.get("maintenance.backup_retention_days", "7"),
                settings.getBoolean("maintenance.auto_backup_enabled", false),
                settings.get("maintenance.backup_cron", "0 0 2 * * ?"));
    }

    @Transactional
    public void saveSettings(MaintenanceDtos.MaintenanceSettingsRequest req) {
        settings.put("maintenance.google_drive_enabled", String.valueOf(req.googleDriveEnabled()));
        settings.put("maintenance.google_drive_folder", safe(req.googleDriveFolder(), "Backups"));
        settings.put("maintenance.backup_retention_days", safe(req.backupRetentionDays(), "7"));
        settings.put("maintenance.auto_backup_enabled", String.valueOf(req.autoBackupEnabled()));
        settings.put("maintenance.backup_cron", safe(req.backupCron(), "0 0 2 * * ?"));
    }

    public void optimizeDb() {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
             String url = conn.getMetaData().getURL();
             if (url.contains("h2")) {
                 stmt.execute("CHECKPOINT");
             }
        } catch (Exception e) {
            throw new RuntimeException("Error optimizing DB", e);
        }
    }

    private String safe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private MaintenanceDtos.BackupJobResponse toDto(MaintBackupJob job) {
        return new MaintenanceDtos.BackupJobResponse(
            job.getId(), job.getBackupType(), job.getStatus(), job.getFilePath(),
            job.getFileSizeBytes(), job.getNotes(), job.getTriggeredBy(),
            job.getStartedAt(), job.getFinishedAt(), job.getCreatedAt()
        );
    }
}

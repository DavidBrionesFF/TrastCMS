package com.nattechnologies.trastcms.plugins.maintenance;

import java.time.Instant;
import java.util.List;

public class MaintenanceDtos {

    public record BackupJobResponse(String id, String backupType, String status, String filePath, Long fileSizeBytes, String notes, String triggeredBy, Instant startedAt, Instant finishedAt, Instant createdAt) {}

    public record BackupRequest(String backupType) {}

    public record CacheInfo(String name, long size, String description) {}

    public record CacheStatsResponse(List<CacheInfo> caches) {}

    public record HealthResponse(double cpuUsage, long heapUsedMb, long heapMaxMb, long uptimeSeconds, boolean dbConnected, String javaVersion, String osName) {}

    public record ScheduleResponse(String id, String scheduleKey, String cronExpression, boolean enabled, Instant lastRunAt, Instant updatedAt) {}

    public record ScheduleRequest(String cronExpression, boolean enabled) {}

    public record GoogleDriveConfigRequest(String clientId, String clientSecret, String refreshToken, boolean enabled) {}

    public record MaintenanceSettingsResponse(boolean googleDriveEnabled, String googleDriveFolder, String backupRetentionDays, boolean autoBackupEnabled, String backupCron) {}

    public record MaintenanceSettingsRequest(boolean googleDriveEnabled, String googleDriveFolder, String backupRetentionDays, boolean autoBackupEnabled, String backupCron) {}
}

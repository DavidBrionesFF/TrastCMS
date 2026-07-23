package com.nattechnologies.trastcms.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "trastcms")
public class TrastCmsProperties {
    private String dataDir = "./data";
    private String baseUrl = "http://localhost:8080";
    private final Admin admin = new Admin();
    private final Plugins plugins = new Plugins();
    private final Media media = new Media();
    private final Mcp mcp = new Mcp();

    public String getDataDir() { return dataDir; }
    public void setDataDir(String dataDir) { this.dataDir = dataDir; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public Admin getAdmin() { return admin; }
    public Plugins getPlugins() { return plugins; }
    public Media getMedia() { return media; }
    public Mcp getMcp() { return mcp; }

    public static class Admin {
        private String email = "admin@trastcms.local";
        private String password = "ChangeMeNow-2026!";
        private String name = "Administrador";
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }

    public static class Plugins {
        private String masterKey = "change-this-plugin-master-key-before-production";
        private int connectTimeoutSeconds = 5;
        private int requestTimeoutSeconds = 10;
        private boolean javaEnabled = true;
        private String directory = "plugins";

        public String getMasterKey() { return masterKey; }
        public void setMasterKey(String masterKey) { this.masterKey = masterKey; }
        public int getConnectTimeoutSeconds() { return connectTimeoutSeconds; }
        public void setConnectTimeoutSeconds(int connectTimeoutSeconds) { this.connectTimeoutSeconds = connectTimeoutSeconds; }
        public int getRequestTimeoutSeconds() { return requestTimeoutSeconds; }
        public void setRequestTimeoutSeconds(int requestTimeoutSeconds) { this.requestTimeoutSeconds = requestTimeoutSeconds; }
        public boolean isJavaEnabled() { return javaEnabled; }
        public void setJavaEnabled(boolean javaEnabled) { this.javaEnabled = javaEnabled; }
        public String getDirectory() { return directory; }
        public void setDirectory(String directory) { this.directory = directory; }
    }

    public static class Mcp {
        private boolean enabled = true;
        private String endpoint = "/mcp";
        private String token = "";
        private String serverVersion = "2.5.0-alpha.7";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getEndpoint() { return endpoint; }
        public String normalizedEndpoint() {
            String value = endpoint == null || endpoint.isBlank()
                    ? "/mcp"
                    : endpoint.trim();
            return value.startsWith("/") ? value : "/" + value;
        }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getServerVersion() { return serverVersion; }
        public void setServerVersion(String serverVersion) { this.serverVersion = serverVersion; }
    }

    public static class Media {
        private long maxBytes = 100L * 1024 * 1024;
        private int maxBatchFiles = 20;
        public long getMaxBytes() { return maxBytes; }
        public void setMaxBytes(long maxBytes) { this.maxBytes = maxBytes; }
        public int getMaxBatchFiles() { return maxBatchFiles; }
        public void setMaxBatchFiles(int maxBatchFiles) { this.maxBatchFiles = maxBatchFiles; }
    }
}

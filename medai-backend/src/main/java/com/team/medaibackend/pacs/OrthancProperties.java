package com.team.medaibackend.pacs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for Orthanc PACS server connection.
 *
 * Orthanc is an open-source, lightweight DICOM server.
 * Download: https://www.orthanc-server.com/
 *
 * Default Orthanc REST API runs on port 8042.
 * Default DICOM port is 4242.
 */
@Configuration
@ConfigurationProperties(prefix = "orthanc")
public class OrthancProperties {

    /**
     * Base URL of the Orthanc REST API (e.g., http://localhost:8042)
     */
    private String url = "http://localhost:8042";

    /**
     * Orthanc REST API username
     */
    private String username = "orthanc";

    /**
     * Orthanc REST API password
     */
    private String password = "orthanc";

    /**
     * Application Entity Title for DICOM communications
     */
    private String aet = "MEDAI";

    /**
     * Connection timeout in seconds
     */
    private Integer timeoutSeconds = 30;

    /**
     * Whether Orthanc integration is enabled
     */
    private Boolean enabled = false;

    // Getters and Setters
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getAet() { return aet; }
    public void setAet(String aet) { this.aet = aet; }

    public Integer getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(Integer timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }

    /**
     * Get the full REST API URL for a specific endpoint
     */
    public String getApiUrl(String endpoint) {
        String base = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        String path = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
        return base + path;
    }
}
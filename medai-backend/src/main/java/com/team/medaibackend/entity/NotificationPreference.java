package com.team.medaibackend.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

@Entity
@Table(name = "notification_preferences")
public class NotificationPreference {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true, nullable = false)
    private User user;

    @Column(name = "enable_in_app")
    private Boolean enableInApp = true;

    @Column(name = "enable_email")
    private Boolean enableEmail = true;

    @Column(name = "enable_push")
    private Boolean enablePush = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "type_preferences", columnDefinition = "jsonb")
    private Map<String, Object> typePreferences;

    @Column(name = "quiet_hours_enabled")
    private Boolean quietHoursEnabled = false;

    @Column(name = "quiet_hours_start")
    private LocalTime quietHoursStart;

    @Column(name = "quiet_hours_end")
    private LocalTime quietHoursEnd;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Boolean getEnableInApp() { return enableInApp; }
    public void setEnableInApp(Boolean enableInApp) { this.enableInApp = enableInApp; }
    public Boolean getEnableEmail() { return enableEmail; }
    public void setEnableEmail(Boolean enableEmail) { this.enableEmail = enableEmail; }
    public Boolean getEnablePush() { return enablePush; }
    public void setEnablePush(Boolean enablePush) { this.enablePush = enablePush; }
    public Map<String, Object> getTypePreferences() { return typePreferences; }
    public void setTypePreferences(Map<String, Object> typePreferences) { this.typePreferences = typePreferences; }
    public Boolean getQuietHoursEnabled() { return quietHoursEnabled; }
    public void setQuietHoursEnabled(Boolean quietHoursEnabled) { this.quietHoursEnabled = quietHoursEnabled; }
    public LocalTime getQuietHoursStart() { return quietHoursStart; }
    public void setQuietHoursStart(LocalTime quietHoursStart) { this.quietHoursStart = quietHoursStart; }
    public LocalTime getQuietHoursEnd() { return quietHoursEnd; }
    public void setQuietHoursEnd(LocalTime quietHoursEnd) { this.quietHoursEnd = quietHoursEnd; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
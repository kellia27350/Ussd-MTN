package com.example.productcatalogue.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ussd_sessions")
public class UssdSessionRecord {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_ENDED = "ENDED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sessionId;

    @Column(nullable = false)
    private String phoneNumber;

    private String language;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime endedAt;
    private String currentLevel;
    private Long categoryId;
    private Integer currentPage;
    private LocalDateTime lastActivityAt;
    private String endReason;

    private String serviceCode;
    @Column(length = 20)
    private String channel;
    private String cellId;
    private String imsi;
    private Long lastProductId;
    private String lastProductName;

    public UssdSessionRecord() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getEndedAt() { return endedAt; }
    public void setEndedAt(LocalDateTime endedAt) { this.endedAt = endedAt; }
    public String getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(String currentLevel) { this.currentLevel = currentLevel; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Integer getCurrentPage() { return currentPage; }
    public void setCurrentPage(Integer currentPage) { this.currentPage = currentPage; }
    public LocalDateTime getLastActivityAt() { return lastActivityAt; }
    public void setLastActivityAt(LocalDateTime lastActivityAt) { this.lastActivityAt = lastActivityAt; }
    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String v) { this.serviceCode = v; }
    public String getChannel() { return channel; }
    public void setChannel(String v) { this.channel = v; }
    public String getCellId() { return cellId; }
    public void setCellId(String v) { this.cellId = v; }
    public String getImsi() { return imsi; }
    public void setImsi(String v) { this.imsi = v; }
    public Long getLastProductId() { return lastProductId; }
    public void setLastProductId(Long v) { this.lastProductId = v; }
    public String getLastProductName() { return lastProductName; }
    public void setLastProductName(String v) { this.lastProductName = v; }
    public String getEndReason() { return endReason; }
    public void setEndReason(String endReason) { this.endReason = endReason; }
}

package com.example.productcatalogue.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ussd_dial_logs", indexes = {
        @Index(name = "idx_dial_phone", columnList = "phoneNumber"),
        @Index(name = "idx_dial_session", columnList = "sessionId")
})
public class UssdDialLog {

    public static final String OUTCOME_OK = "OK";
    public static final String OUTCOME_FAILED = "FAILED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String phoneNumber;

    private String serviceCode;
    private Integer requestId;
    private String sessionId;

    @Column(length = 500)
    private String inputText;

    @Column(nullable = false)
    private String outcome;

    @Column(length = 2000)
    private String responseMessage;

    @Column(length = 500)
    private String errorMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public UssdDialLog() {}

    public Long getId() { return id; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String v) { this.phoneNumber = v; }
    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String v) { this.serviceCode = v; }
    public Integer getRequestId() { return requestId; }
    public void setRequestId(Integer v) { this.requestId = v; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String v) { this.sessionId = v; }
    public String getInputText() { return inputText; }
    public void setInputText(String v) { this.inputText = v; }
    public String getOutcome() { return outcome; }
    public void setOutcome(String v) { this.outcome = v; }
    public String getResponseMessage() { return responseMessage; }
    public void setResponseMessage(String v) { this.responseMessage = v; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String v) { this.errorMessage = v; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }
}

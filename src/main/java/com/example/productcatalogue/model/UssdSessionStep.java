package com.example.productcatalogue.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ussd_session_steps")
public class UssdSessionStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String sessionId;

    @Column(nullable = false)
    private String inputText;

    @Column(nullable = false)
    private String level;

    @Column(nullable = false, length = 2000)
    private String responseMessage;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public UssdSessionStep() {}

    public UssdSessionStep(String sessionId, String inputText, String level, String responseMessage) {
        this.sessionId = sessionId;
        this.inputText = inputText;
        this.level = level;
        this.responseMessage = responseMessage;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getSessionId() { return sessionId; }
    public String getInputText() { return inputText; }
    public String getLevel() { return level; }
    public String getResponseMessage() { return responseMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}

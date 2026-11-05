package com.example.productcatalogue.dto;

public class ChatResponse {

    private String sessionId;
    private String message;
    private boolean ended;

    public ChatResponse() {}

    public ChatResponse(String sessionId, String message, boolean ended) {
        this.sessionId = sessionId;
        this.message = message;
        this.ended = ended;
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isEnded() { return ended; }
    public void setEnded(boolean ended) { this.ended = ended; }
}

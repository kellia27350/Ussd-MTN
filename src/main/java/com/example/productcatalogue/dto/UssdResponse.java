package com.example.productcatalogue.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "ussdResponse")
public class UssdResponse {

    private String sessionId;
    private String message;
    private boolean error;

    public UssdResponse() {}

    public UssdResponse(String sessionId, String message, boolean error) {
        this.sessionId = sessionId;
        this.message = message;
        this.error = error;
    }

    public static UssdResponse ok(String sessionId, String message) {
        return new UssdResponse(sessionId, message, false);
    }

    public static UssdResponse error(String message) {
        return new UssdResponse(null, message, true);
    }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isError() { return error; }
    public void setError(boolean error) { this.error = error; }
}

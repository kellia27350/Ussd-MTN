package com.example.productcatalogue.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@JacksonXmlRootElement(localName = "ussdRequest")
public class UssdRequest {

    @NotNull(message = "requestId is required")
    @Min(value = 0, message = "requestId must be 0 (continue) or 1 (new session)")
    @Max(value = 1, message = "requestId must be 0 (continue) or 1 (new session)")
    private Integer requestId;

    private String sessionId;
    private String serviceCode;

    @NotBlank(message = "phoneNumber is required")
    private String phoneNumber;

    private String text;

    public UssdRequest() {}

    public Integer getRequestId() { return requestId; }
    public void setRequestId(Integer requestId) { this.requestId = requestId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getServiceCode() { return serviceCode; }
    public void setServiceCode(String serviceCode) { this.serviceCode = serviceCode; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
}

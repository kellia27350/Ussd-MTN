package com.example.productcatalogue.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "response")
public class MtnUssdResponse {

    @JacksonXmlProperty(isAttribute = true)
    private String type = "push";

    @JacksonXmlProperty(localName = "sessionid")
    private String sessionid;

    @JacksonXmlProperty(localName = "msisdn")
    private String msisdn;

    @JacksonXmlProperty(localName = "freeflow")
    private Freeflow freeflow;

    public MtnUssdResponse() {}

    public static MtnUssdResponse from(UssdResponse ussdResponse, String msisdn) {
        MtnUssdResponse mtn = new MtnUssdResponse();
        mtn.setSessionid(ussdResponse.getSessionId());
        mtn.setMsisdn(msisdn);
        String message = ussdResponse.getMessage() != null ? ussdResponse.getMessage() : "";
        mtn.setFreeflow(new Freeflow(message.startsWith("END") ? "FD" : "FC", message));
        return mtn;
    }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getSessionid() { return sessionid; }
    public void setSessionid(String sessionid) { this.sessionid = sessionid; }
    public String getMsisdn() { return msisdn; }
    public void setMsisdn(String msisdn) { this.msisdn = msisdn; }
    public Freeflow getFreeflow() { return freeflow; }
    public void setFreeflow(Freeflow freeflow) { this.freeflow = freeflow; }

    public static class Freeflow {

        @JacksonXmlProperty(localName = "mode")
        private String mode;

        @JacksonXmlProperty(localName = "message")
        private String message;

        public Freeflow() {}

        public Freeflow(String mode, String message) {
            this.mode = mode;
            this.message = message;
        }

        public String getMode() { return mode; }
        public void setMode(String mode) { this.mode = mode; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}

package com.example.productcatalogue.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "request")
public class MtnUssdRequest {

    @JacksonXmlProperty(localName = "msisdn")
    private String msisdn;

    @JacksonXmlProperty(localName = "input")
    private String input;

    @JacksonXmlProperty(localName = "sessionid")
    private String sessionid;

    @JacksonXmlProperty(localName = "new_request")
    private Integer newRequest;

    @JacksonXmlProperty(localName = "CellID")
    private String cellId;

    @JacksonXmlProperty(localName = "imsi")
    private String imsi;

    @JacksonXmlProperty(localName = "parameters")
    private String parameters;

    public UssdRequest toUssdRequest() {
        UssdRequest req = new UssdRequest();
        req.setPhoneNumber(msisdn != null ? msisdn.trim() : null);
        req.setText(input != null ? input.trim() : "");
        req.setSessionId(sessionid != null && !sessionid.isBlank() ? sessionid.trim() : null);
        req.setRequestId(newRequest != null ? newRequest : 1);
        req.setServiceCode("*140#");
        return req;
    }

    public String getMsisdn() { return msisdn; }
    public void setMsisdn(String msisdn) { this.msisdn = msisdn; }
    public String getInput() { return input; }
    public void setInput(String input) { this.input = input; }
    public String getSessionid() { return sessionid; }
    public void setSessionid(String sessionid) { this.sessionid = sessionid; }
    public Integer getNewRequest() { return newRequest; }
    public void setNewRequest(Integer newRequest) { this.newRequest = newRequest; }
    public String getCellId() { return cellId; }
    public void setCellId(String cellId) { this.cellId = cellId; }
    public String getImsi() { return imsi; }
    public void setImsi(String imsi) { this.imsi = imsi; }
    public String getParameters() { return parameters; }
    public void setParameters(String parameters) { this.parameters = parameters; }
}

package com.example.productcatalogue.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "response")
public class GatewayResponse {
    public String msisdn;
    public String sessionid;
    public Freeflow freeflow = new Freeflow();

    @JacksonXmlProperty(localName = "applicationResponse")
    public String applicationResponse;

    public static class Freeflow {
        public String mode;
    }
}

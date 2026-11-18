package com.example.productcatalogue.dto;

import com.fasterxml.jackson.annotation.JsonRootName;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonRootName("response")
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

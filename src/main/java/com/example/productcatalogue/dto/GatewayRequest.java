package com.example.productcatalogue.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRootName;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonRootName("request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GatewayRequest {
    public String msisdn;
    public String input;
    public String sessionid;

    @JacksonXmlProperty(localName = "new_request")
    public String newRequest;
}

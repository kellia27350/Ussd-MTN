package com.example.productcatalogue.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "request")
@JsonIgnoreProperties(ignoreUnknown = true)
public class GatewayRequest {
    public String msisdn;
    public String input;
    public String sessionid;

    @JacksonXmlProperty(localName = "new_request")
    public String newRequest;
}

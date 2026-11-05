package com.example.productcatalogue.controller;

import com.example.productcatalogue.dto.GatewayRequest;
import com.example.productcatalogue.dto.GatewayResponse;
import com.example.productcatalogue.dto.UssdRequest;
import com.example.productcatalogue.dto.UssdResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class GatewayController {

    private final UssdController ussd;
    private final Map<String, String> sessions = new ConcurrentHashMap<>();

    public GatewayController(UssdController ussd) {
        this.ussd = ussd;
    }

    @PostMapping(value = "/gateway/ussd", consumes = {"application/xml", "text/xml"}, produces = "application/xml")
    public GatewayResponse handle(@RequestBody GatewayRequest in) {
        boolean isNew = "1".equals(in.newRequest);

        UssdRequest r = new UssdRequest();
        r.setPhoneNumber(in.msisdn);
        r.setServiceCode("*140#");

        if (isNew) {
            r.setRequestId(1);
            r.setText("");
        } else {
            r.setRequestId(0);
            r.setText(in.input == null ? "" : in.input.trim());
            r.setSessionId(sessions.get(in.sessionid));
        }

        GatewayResponse res = new GatewayResponse();
        res.msisdn = in.msisdn;
        res.sessionid = in.sessionid;

        try {
            UssdResponse out = ussd.handleUssdJsonOrXml(r).getBody();
            sessions.put(in.sessionid, out.getSessionId());
            boolean ended = out.getMessage().startsWith("END");
            res.freeflow.mode = ended ? "FB" : "FC";
            res.applicationResponse = ended ? out.getMessage().substring(3).trim() : out.getMessage();
            if (ended) sessions.remove(in.sessionid);
        } catch (RuntimeException ex) {
            res.freeflow.mode = "FB";
            res.applicationResponse = "Error: " + ex.getMessage();
            sessions.remove(in.sessionid);
        }

        return res;
    }
}

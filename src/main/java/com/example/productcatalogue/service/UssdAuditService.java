package com.example.productcatalogue.service;

import com.example.productcatalogue.dto.UssdRequest;
import com.example.productcatalogue.model.UssdDialLog;
import com.example.productcatalogue.repository.UssdDialLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UssdAuditService {

    private final UssdDialLogRepository repository;

    public UssdAuditService(UssdDialLogRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UssdRequest request, String sessionId, String outcome,
                       String responseMessage, String errorMessage) {
        UssdDialLog log = new UssdDialLog();
        log.setPhoneNumber(request.getPhoneNumber());
        log.setServiceCode(request.getServiceCode());
        log.setRequestId(request.getRequestId());
        log.setSessionId(sessionId != null ? sessionId : request.getSessionId());
        log.setInputText(cap(request.getText(), 500));
        log.setOutcome(outcome);
        log.setResponseMessage(cap(responseMessage, 2000));
        log.setErrorMessage(cap(errorMessage, 500));
        repository.save(log);
    }

    private String cap(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}

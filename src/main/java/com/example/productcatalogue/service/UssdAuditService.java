package com.example.productcatalogue.service;

import com.example.productcatalogue.dto.UssdRequest;
import com.example.productcatalogue.model.UssdDialLog;
import com.example.productcatalogue.repository.UssdDialLogRepository;
import com.example.productcatalogue.repository.UssdSessionRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UssdAuditService {

    private final UssdDialLogRepository repository;
    private final UssdSessionRecordRepository sessionRecordRepository;

    public UssdAuditService(UssdDialLogRepository repository,
                            UssdSessionRecordRepository sessionRecordRepository) {
        this.repository = repository;
        this.sessionRecordRepository = sessionRecordRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UssdRequest request, String sessionId, String outcome,
                       String responseMessage, String errorMessage) {
        UssdDialLog log = new UssdDialLog();
        log.setPhoneNumber(request.getPhoneNumber());
        log.setServiceCode(request.getServiceCode());
        log.setRequestId(request.getRequestId());
        log.setSessionId(sessionId != null ? sessionId : request.getSessionId());
        log.setChannel(request.getChannel());
        log.setCellId(request.getCellId());
        log.setImsi(request.getImsi());
        log.setInputText(cap(request.getText(), 500));
        log.setOutcome(outcome);
        log.setResponseMessage(cap(responseMessage, 2000));
        log.setErrorMessage(cap(errorMessage, 500));
        repository.save(log);

        // Copy channel / cell / imsi onto the session the first time we see it.
        if (log.getSessionId() != null) {
            sessionRecordRepository.findBySessionId(log.getSessionId()).ifPresent(r -> {
                if (r.getChannel() == null) {
                    r.setChannel(request.getChannel());
                    r.setCellId(request.getCellId());
                    r.setImsi(request.getImsi());
                    r.setServiceCode(log.getServiceCode() != null ? log.getServiceCode() : "*140#");
                    sessionRecordRepository.save(r);
                }
            });
        }
    }

    private String cap(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}

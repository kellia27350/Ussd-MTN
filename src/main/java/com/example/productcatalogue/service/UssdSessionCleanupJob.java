package com.example.productcatalogue.service;

import com.example.productcatalogue.model.UssdSessionRecord;
import com.example.productcatalogue.model.UssdSessionStep;
import com.example.productcatalogue.repository.UssdSessionRecordRepository;
import com.example.productcatalogue.repository.UssdSessionStepRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class UssdSessionCleanupJob {

    private final UssdSessionRecordRepository sessionRepo;
    private final UssdSessionStore sessionStore;
    private final UssdSessionStepRepository stepRepo;

    public UssdSessionCleanupJob(UssdSessionRecordRepository sessionRepo,
                                 UssdSessionStore sessionStore,
                                 UssdSessionStepRepository stepRepo) {
        this.sessionRepo = sessionRepo;
        this.sessionStore = sessionStore;
        this.stepRepo = stepRepo;
    }

    @Scheduled(fixedRate = 60_000)
    public void closeExpiredSessions() {
        for (UssdSessionRecord record : sessionRepo.findByStatus(UssdSessionRecord.STATUS_ACTIVE)) {
            if (sessionStore.get(record.getSessionId()) == null) {
                record.setStatus(UssdSessionRecord.STATUS_ENDED);
                record.setEndedAt(LocalDateTime.now());
                record.setEndReason("TIMEOUT");
                sessionRepo.save(record);
                stepRepo.save(new UssdSessionStep(
                        record.getSessionId(), "",
                        record.getCurrentLevel() == null ? "UNKNOWN" : record.getCurrentLevel(),
                        "END Session timed out"));
            }
        }
    }
}

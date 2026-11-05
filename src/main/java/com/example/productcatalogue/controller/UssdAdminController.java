package com.example.productcatalogue.controller;

import com.example.productcatalogue.model.UssdDialLog;
import com.example.productcatalogue.model.UssdSessionRecord;
import com.example.productcatalogue.model.UssdSessionStep;
import com.example.productcatalogue.repository.UssdDialLogRepository;
import com.example.productcatalogue.repository.UssdSessionRecordRepository;
import com.example.productcatalogue.repository.UssdSessionStepRepository;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ussd")
public class UssdAdminController {

    private final UssdDialLogRepository dialLogRepo;
    private final UssdSessionRecordRepository sessionRepo;
    private final UssdSessionStepRepository stepRepo;

    public UssdAdminController(UssdDialLogRepository dialLogRepo,
                               UssdSessionRecordRepository sessionRepo,
                               UssdSessionStepRepository stepRepo) {
        this.dialLogRepo = dialLogRepo;
        this.sessionRepo = sessionRepo;
        this.stepRepo = stepRepo;
    }

    @GetMapping("/dial-logs")
    public List<UssdDialLog> dialLogs(@RequestParam(required = false) String phoneNumber) {
        return phoneNumber == null || phoneNumber.isBlank()
                ? dialLogRepo.findAllByOrderByCreatedAtDesc()
                : dialLogRepo.findByPhoneNumberOrderByCreatedAtDesc(phoneNumber);
    }

    @GetMapping("/callers")
    public List<Map<String, Object>> callers() {
        return dialLogRepo.summarizeCallers().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("phoneNumber", r[0]);
            m.put("totalDials", r[1]);
            m.put("firstSeen", r[2]);
            m.put("lastSeen", r[3]);
            return m;
        }).toList();
    }

    @GetMapping("/sessions")
    public List<UssdSessionRecord> sessions(@RequestParam(required = false) String phoneNumber,
                                            @RequestParam(required = false) String status) {
        List<UssdSessionRecord> list = phoneNumber == null || phoneNumber.isBlank()
                ? sessionRepo.findAllByOrderByStartedAtDesc()
                : sessionRepo.findByPhoneNumberOrderByStartedAtDesc(phoneNumber);
        if (status != null && !status.isBlank()) {
            list = list.stream().filter(r -> status.equalsIgnoreCase(r.getStatus())).toList();
        }
        return list;
    }

    @GetMapping("/sessions/{sessionId}/steps")
    public List<UssdSessionStep> steps(@PathVariable String sessionId) {
        return stepRepo.findBySessionIdOrderByCreatedAtAsc(sessionId);
    }
}

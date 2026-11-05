package com.example.productcatalogue.repository;

import com.example.productcatalogue.model.UssdSessionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

public interface UssdSessionRecordRepository extends JpaRepository<UssdSessionRecord, Long> {

    Optional<UssdSessionRecord> findBySessionId(String sessionId);

    Optional<UssdSessionRecord> findFirstByPhoneNumberAndStatusOrderByStartedAtDesc(
            String phoneNumber, String status);

    void deleteByStatusAndEndedAtBefore(String status, LocalDateTime before);

    List<UssdSessionRecord> findByStatus(String status);

    List<UssdSessionRecord> findByPhoneNumberAndStatus(String phoneNumber, String status);

    List<UssdSessionRecord> findByPhoneNumberOrderByStartedAtDesc(String phoneNumber);

    List<UssdSessionRecord> findAllByOrderByStartedAtDesc();
}

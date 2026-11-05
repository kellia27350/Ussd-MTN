package com.example.productcatalogue.repository;

import com.example.productcatalogue.model.UssdSessionStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UssdSessionStepRepository extends JpaRepository<UssdSessionStep, Long> {
    void deleteBySessionId(String sessionId);

    List<UssdSessionStep> findBySessionIdOrderByCreatedAtAsc(String sessionId);
}

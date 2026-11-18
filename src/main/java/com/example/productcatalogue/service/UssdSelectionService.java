package com.example.productcatalogue.service;

import com.example.productcatalogue.model.Product;
import com.example.productcatalogue.model.UssdProductSelection;
import com.example.productcatalogue.repository.UssdProductSelectionRepository;
import com.example.productcatalogue.repository.UssdSessionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class UssdSelectionService {

    private static final Logger log = LoggerFactory.getLogger(UssdSelectionService.class);

    private final UssdProductSelectionRepository selectionRepository;
    private final UssdSessionRecordRepository sessionRecordRepository;

    public UssdSelectionService(UssdProductSelectionRepository selectionRepository,
                                UssdSessionRecordRepository sessionRecordRepository) {
        this.selectionRepository = selectionRepository;
        this.sessionRecordRepository = sessionRecordRepository;
    }

    /**
     * Records that the caller opened/viewed a product. Swallows errors: a logging
     * problem must never break the caller's USSD session.
     */
    public void record(String sessionId, String phoneNumber, String language,
                       Product product, String selectionType) {
        try {
            UssdProductSelection sel = new UssdProductSelection();
            sel.setSessionId(sessionId);
            sel.setPhoneNumber(phoneNumber);
            sel.setSelectionType(selectionType);
            sel.setLanguage(language);
            sel.setProductId(product.getId());
            sel.setProductName(product.getName());
            sel.setParentProductId(product.getParentId());
            sel.setPrice(product.getPrice());
            sel.setValidityDays(product.getValidityDays());
            if (product.getCategory() != null) {
                sel.setCategoryId(product.getCategory().getId());
                sel.setCategoryName(product.getCategory().getName());
            }
            selectionRepository.save(sel);

            sessionRecordRepository.findBySessionId(sessionId).ifPresent(r -> {
                r.setLastProductId(product.getId());
                r.setLastProductName(product.getName());
                r.setLastActivityAt(LocalDateTime.now());
                sessionRecordRepository.save(r);
            });
        } catch (RuntimeException ex) {
            log.warn("Could not record USSD product selection for session {}: {}",
                    sessionId, ex.getMessage());
        }
    }
}

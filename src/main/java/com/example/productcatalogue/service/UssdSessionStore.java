package com.example.productcatalogue.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UssdSessionStore {

    public enum Level {
        LANGUAGE_SELECTION,
        CATEGORY_LIST,
        PRODUCT_LIST,
        RESUME_DECISION,
        PAYMENT_METHOD,
        MOMO_PIN,
        LOAN_OFFER,
        LOAN_CONFIRM
    }

    public static class Session {
        private Level level;
        private Long categoryId;
        private int page;
        private final String phoneNumber;
        private com.example.productcatalogue.model.Language language;
        private Long pendingProductId;
        private Long parentProductId;
        private int productPage;
        private String endReason;
        private Instant lastAccessedAt;

        public Session(Level level, Long categoryId, String phoneNumber) {
            this.level = level;
            this.categoryId = categoryId;
            this.phoneNumber = phoneNumber;
            this.page = 0;
            this.lastAccessedAt = Instant.now();
        }

        public void touch() { this.lastAccessedAt = Instant.now(); }

        public Level getLevel() { return level; }
        public void setLevel(Level level) { this.level = level; }
        public Long getCategoryId() { return categoryId; }
        public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public String getPhoneNumber() { return phoneNumber; }
        public com.example.productcatalogue.model.Language getLanguage() { return language; }
        public void setLanguage(com.example.productcatalogue.model.Language language) { this.language = language; }
        public Long getPendingProductId() { return pendingProductId; }
        public void setPendingProductId(Long pendingProductId) { this.pendingProductId = pendingProductId; }
        public Long getParentProductId() { return parentProductId; }
        public void setParentProductId(Long parentProductId) { this.parentProductId = parentProductId; }
        public int getProductPage() { return productPage; }
        public void setProductPage(int productPage) { this.productPage = productPage; }
        public String getEndReason() { return endReason; }
        public void setEndReason(String endReason) { this.endReason = endReason; }
        public Instant getLastAccessedAt() { return lastAccessedAt; }
    }

    private final Duration ttl;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public UssdSessionStore(@Value("${app.ussd.session-ttl-seconds:180}") long ttlSeconds) {
        this.ttl = Duration.ofSeconds(ttlSeconds);
    }

    public Duration getTtl() { return ttl; }

    public String newSessionId() { return UUID.randomUUID().toString(); }

    public Session get(String sessionId) {
        if (sessionId == null) return null;
        Session session = sessions.get(sessionId);
        if (session == null) return null;
        if (Duration.between(session.getLastAccessedAt(), Instant.now()).compareTo(ttl) > 0) {
            sessions.remove(sessionId);
            return null;
        }
        return session;
    }

    public void put(String sessionId, Session session) { sessions.put(sessionId, session); }
    public void remove(String sessionId) { sessions.remove(sessionId); }
}

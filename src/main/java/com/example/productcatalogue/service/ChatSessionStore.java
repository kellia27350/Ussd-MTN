package com.example.productcatalogue.service;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatSessionStore {

    public static final Duration SESSION_TTL = Duration.ofMinutes(15);

    public enum Level {
        CATEGORY_LIST,
        PRODUCT_LIST
    }

    public static class Session {
        private Level level;
        private Long categoryId;
        private int page;
        private Instant lastAccessedAt;

        public Session(Level level, Long categoryId) {
            this.level = level;
            this.categoryId = categoryId;
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
        public Instant getLastAccessedAt() { return lastAccessedAt; }
    }

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();

    public String newSessionId() { return UUID.randomUUID().toString(); }

    public Session get(String sessionId) {
        if (sessionId == null) return null;
        Session session = sessions.get(sessionId);
        if (session == null) return null;
        if (Duration.between(session.getLastAccessedAt(), Instant.now()).compareTo(SESSION_TTL) > 0) {
            sessions.remove(sessionId);
            return null;
        }
        return session;
    }

    public void put(String sessionId, Session session) { sessions.put(sessionId, session); }
    public void remove(String sessionId) { sessions.remove(sessionId); }
}

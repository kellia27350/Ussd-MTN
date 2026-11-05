package com.example.productcatalogue.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class UssdProgressStore {

    public static class SavedProgress {
        private final UssdSessionStore.Level level;
        private final Long categoryId;
        private final int page;
        private final com.example.productcatalogue.model.Language language;

        public SavedProgress(UssdSessionStore.Level level, Long categoryId, int page,
                             com.example.productcatalogue.model.Language language) {
            this.level = level;
            this.categoryId = categoryId;
            this.page = page;
            this.language = language;
        }

        public SavedProgress(UssdSessionStore.Level level, Long categoryId, int page) {
            this(level, categoryId, page, null);
        }

        public UssdSessionStore.Level getLevel() { return level; }
        public Long getCategoryId() { return categoryId; }
        public int getPage() { return page; }
        public com.example.productcatalogue.model.Language getLanguage() { return language; }
    }

    private final Map<String, SavedProgress> progress = new ConcurrentHashMap<>();

    public SavedProgress get(String phoneNumber) { return progress.get(phoneNumber); }
    public void put(String phoneNumber, SavedProgress saved) { progress.put(phoneNumber, saved); }
    public void remove(String phoneNumber) { progress.remove(phoneNumber); }
}

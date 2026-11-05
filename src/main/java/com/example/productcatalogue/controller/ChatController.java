package com.example.productcatalogue.controller;

import com.example.productcatalogue.dto.ChatRequest;
import com.example.productcatalogue.dto.ChatResponse;
import com.example.productcatalogue.model.Category;
import com.example.productcatalogue.model.Product;
import com.example.productcatalogue.repository.CategoryRepository;
import com.example.productcatalogue.repository.ProductRepository;
import com.example.productcatalogue.service.ChatSessionStore;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
public class ChatController {

    private static final int PAGE_SIZE = 5;

    private final CategoryRepository categoryRepo;
    private final ProductRepository productRepo;
    private final ChatSessionStore store;

    public ChatController(CategoryRepository categoryRepo, ProductRepository productRepo, ChatSessionStore store) {
        this.categoryRepo = categoryRepo;
        this.productRepo = productRepo;
        this.store = store;
    }

    @PostMapping("/api/chat")
    public ChatResponse chat(@RequestBody ChatRequest request) {
        List<Category> categories = categoryRepo.findAllByOrderByNameAsc();
        String choice = request.getMessage() != null ? request.getMessage().trim() : "";
        String sessionId = request.getSessionId();
        ChatSessionStore.Session session = store.get(sessionId);

        if (session == null) {
            sessionId = store.newSessionId();
            session = new ChatSessionStore.Session(ChatSessionStore.Level.CATEGORY_LIST, null);
            store.put(sessionId, session);
            return new ChatResponse(sessionId, buildCategoryPage(categories, 0), false);
        }

        session.touch();
        return session.getLevel() == ChatSessionStore.Level.CATEGORY_LIST
                ? handleCategories(sessionId, session, categories, choice)
                : handleProducts(sessionId, session, choice);
    }

    private ChatResponse handleCategories(String sessionId, ChatSessionStore.Session session,
                                          List<Category> categories, String choice) {
        int page = session.getPage();
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, categories.size());

        if ("n".equalsIgnoreCase(choice) && end < categories.size()) {
            session.setPage(page + 1);
            store.put(sessionId, session);
            return new ChatResponse(sessionId, buildCategoryPage(categories, page + 1), false);
        }

        if ("0".equals(choice) && page > 0) {
            session.setPage(page - 1);
            store.put(sessionId, session);
            return new ChatResponse(sessionId, buildCategoryPage(categories, page - 1), false);
        }

        int pick = toInt(choice, -1);
        if (pick < 1 || pick > end - start) {
            store.remove(sessionId);
            return new ChatResponse(sessionId, "Invalid selection. Start a new chat and try again.", true);
        }

        Category selected = categories.get(start + pick - 1);
        List<Product> products = productRepo.findByCategoryIdOrderByNameAsc(selected.getId());
        session.setLevel(ChatSessionStore.Level.PRODUCT_LIST);
        session.setCategoryId(selected.getId());
        store.put(sessionId, session);
        return new ChatResponse(sessionId, buildProductMenu(selected, products), false);
    }

    private ChatResponse handleProducts(String sessionId, ChatSessionStore.Session session, String choice) {
        if ("00".equals(choice)) {
            store.remove(sessionId);
            return new ChatResponse(sessionId, "Thanks for stopping by! 👋", true);
        }

        List<Category> categories = categoryRepo.findAllByOrderByNameAsc();

        if ("0".equals(choice)) {
            session.setLevel(ChatSessionStore.Level.CATEGORY_LIST);
            session.setCategoryId(null);
            store.put(sessionId, session);
            return new ChatResponse(sessionId, buildCategoryPage(categories, session.getPage()), false);
        }

        List<Product> products = productRepo.findByCategoryIdOrderByNameAsc(session.getCategoryId());
        int pick = toInt(choice, -1);
        if (pick < 1 || pick > products.size()) {
            store.remove(sessionId);
            return new ChatResponse(sessionId, "Invalid selection. Start a new chat and try again.", true);
        }

        store.remove(sessionId);
        return new ChatResponse(sessionId, formatProduct(products.get(pick - 1)), true);
    }

    private String buildCategoryPage(List<Category> categories, int page) {
        if (categories.isEmpty()) return "No categories available right now.";

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, categories.size());

        StringBuilder sb = new StringBuilder("Hi! 👋 What would you like to browse?\n");
        for (int i = start; i < end; i++) {
            sb.append(i - start + 1).append(". ").append(categories.get(i).getName()).append("\n");
        }
        if (end < categories.size()) sb.append("n. Next page\n");
        if (page > 0) sb.append("0. Previous page\n");
        return sb.toString().stripTrailing();
    }

    private String buildProductMenu(Category category, List<Product> products) {
        if (products.isEmpty()) return "No products in " + category.getName() + " right now.\n0. Back";
        StringBuilder sb = new StringBuilder(category.getName()).append(":\n");
        for (int i = 0; i < products.size(); i++) {
            sb.append(i + 1).append(". ").append(products.get(i).getName())
                    .append(" - ").append(products.get(i).getPrice()).append("\n");
        }
        return sb.append("0. Back\n00. Exit").toString();
    }

    private String formatProduct(Product p) {
        return p.getName() + "\n"
                + "Category: " + p.getCategory().getName() + "\n"
                + "Price: " + p.getPrice() + "\n"
                + "Validity: " + (p.getValidityDays() == null ? "Until fully used" : p.getValidityDays() + " day(s)") + "\n"
                + (p.getDescription() != null ? p.getDescription() : "");
    }

    private int toInt(String value, int fallback) {
        try { return Integer.parseInt(value.trim()); } catch (Exception e) { return fallback; }
    }
}

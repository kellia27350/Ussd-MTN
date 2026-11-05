package com.example.productcatalogue.controller;

import com.example.productcatalogue.dto.MtnUssdRequest;
import com.example.productcatalogue.dto.MtnUssdResponse;
import com.example.productcatalogue.dto.UssdRequest;
import com.example.productcatalogue.dto.UssdResponse;
import com.example.productcatalogue.exception.InvalidSessionException;
import com.example.productcatalogue.model.Category;
import com.example.productcatalogue.model.Product;
import com.example.productcatalogue.repository.CategoryRepository;
import com.example.productcatalogue.repository.ProductRepository;
import com.example.productcatalogue.model.UssdDialLog;
import com.example.productcatalogue.service.UssdAuditService;
import com.example.productcatalogue.service.UssdProgressStore;
import com.example.productcatalogue.service.UssdSessionStore;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.example.productcatalogue.model.Language;
import com.example.productcatalogue.util.UssdMessages;
import com.example.productcatalogue.model.UssdSessionRecord;
import com.example.productcatalogue.model.UssdSessionStep;
import com.example.productcatalogue.repository.UssdSessionRecordRepository;
import com.example.productcatalogue.repository.UssdSessionStepRepository;
import java.time.LocalDateTime;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.Comparator;


@RestController
public class UssdController {

    private static final int FIRST_PAGE_SIZE = 6;
    private static final int OTHER_PAGE_SIZE = 7;
    private static final int PRODUCT_PAGE_SIZE = 3;

    private static final Map<String, Integer> CATEGORY_PRIORITY = Map.ofEntries(
            Map.entry("mydeal", 0),
            Map.entry("gumamo", 1),
            Map.entry("mtnirekure(24hrs)", 2),
            Map.entry("mtnirekureicyumweru", 3),
            Map.entry("mtnirekureukwezi", 4),
            Map.entry("gwamon'", 5),
            Map.entry("ayousigaranye", 6),
            Map.entry("desade", 7),
            Map.entry("foleva", 8),
            Map.entry("amahanga", 9),
            Map.entry("ihereza", 10),
            Map.entry("changelanguage", 11),
            Map.entry("smspack", 12),
            Map.entry("iwacupromo", 13)
    );

    private static final String CHANGE_LANGUAGE_KEY = "changelanguage";

    private static final Language DEFAULT_LANGUAGE = Language.EN;

    private static String normalizeName(String name) {
        return name == null ? "" : name.toLowerCase().replaceAll("\\s+", "");
    }

    private static int pageStart(int page) {
        return page <= 0 ? 0 : FIRST_PAGE_SIZE + (page - 1) * OTHER_PAGE_SIZE;
    }

    private static int pageEnd(int page, int total) {
        int size = page <= 0 ? FIRST_PAGE_SIZE : OTHER_PAGE_SIZE;
        return Math.min(pageStart(page) + size, total);
    }

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final UssdSessionStore sessionStore;
    private final UssdProgressStore progressStore;
    private final ObjectMapper objectMapper;
    private final UssdSessionRecordRepository sessionRecordRepository;
    private final UssdSessionStepRepository sessionStepRepository;
    private final UssdAuditService auditService;

    public UssdController(CategoryRepository categoryRepository,
                          ProductRepository productRepository,
                          UssdSessionStore sessionStore,
                          UssdProgressStore progressStore,
                          ObjectMapper objectMapper,
                          UssdSessionRecordRepository sessionRecordRepository,
                          UssdSessionStepRepository sessionStepRepository,
                          UssdAuditService auditService) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.sessionStore = sessionStore;
        this.progressStore = progressStore;
        this.objectMapper = objectMapper;
        this.sessionRecordRepository = sessionRecordRepository;
        this.sessionStepRepository = sessionStepRepository;
        this.auditService = auditService;
    }

    @PostMapping(value = "/ussd",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<UssdResponse> handleUssdJsonOrXml(@Valid @RequestBody UssdRequest request) {
        return processUssd(request);
    }

    @PostMapping(value = "/ussd",
            consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
            produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> handleUssdPlainText(@Valid @RequestBody UssdRequest request) {
        ResponseEntity<UssdResponse> result = processUssd(request);
        UssdResponse body = result.getBody();
        String sessionId = (body != null && body.getSessionId() != null) ? body.getSessionId() : "";
        String message = (body != null) ? body.getMessage() : "";
        return ResponseEntity.status(result.getStatusCode())
                .header("X-Session-Id", sessionId)
                .body(message);
    }

    @PostMapping(value = "/ussd",
            consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<UssdResponse> handleUssdForm(@Valid @ModelAttribute UssdRequest request) {
        return processUssd(request);
    }

    @PostMapping(value = "/ussd/mtn",
            consumes = MediaType.APPLICATION_XML_VALUE,
            produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<MtnUssdResponse> handleMtnUssd(@RequestBody String rawXml) {
        try {
            XmlMapper xmlMapper = new XmlMapper();
            MtnUssdRequest mtnRequest = xmlMapper.readValue(rawXml, MtnUssdRequest.class);

            org.slf4j.LoggerFactory.getLogger(UssdController.class)
                    .info("MTN request: msisdn={}, sessionid={}, new_request={}, input={}",
                            mtnRequest.getMsisdn(), mtnRequest.getSessionid(),
                            mtnRequest.getNewRequest(), mtnRequest.getInput());

            UssdRequest ussdRequest = mtnRequest.toUssdRequest();

            if (ussdRequest.getPhoneNumber() == null || ussdRequest.getPhoneNumber().isBlank()) {
                throw new IllegalArgumentException("msisdn (phoneNumber) is required");
            }
            if (ussdRequest.getRequestId() == null
                    || ussdRequest.getRequestId() < 0
                    || ussdRequest.getRequestId() > 1) {
                throw new IllegalArgumentException("new_request must be 0 (continue) or 1 (new session)");
            }

            ResponseEntity<UssdResponse> result = processUssd(ussdRequest);
            UssdResponse body = result.getBody();
            MtnUssdResponse mtnResponse = MtnUssdResponse.from(
                    body != null ? body : UssdResponse.error("No response"),
                    mtnRequest.getMsisdn());
            return ResponseEntity.status(result.getStatusCode()).body(mtnResponse);

        } catch (Exception ex) {
            MtnUssdResponse errorResponse = new MtnUssdResponse();
            errorResponse.setSessionid(null);
            MtnUssdResponse.Freeflow ff = new MtnUssdResponse.Freeflow("FD",
                    "END Error: " + ex.getMessage());
            errorResponse.setFreeflow(ff);
            return ResponseEntity.badRequest().body(errorResponse);
        }
    }

    @PostMapping(value = "/ussd",
            consumes = MediaType.TEXT_PLAIN_VALUE,
            produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
    public ResponseEntity<UssdResponse> handleUssdPlainTextBody(@RequestBody String body) {
        try {
            UssdRequest request = parseUssdRequest(body);
            if (request.getRequestId() == null || request.getPhoneNumber() == null
                    || request.getPhoneNumber().isBlank()) {
                throw new IllegalArgumentException("requestId and phoneNumber are required.");
            }
            if (request.getRequestId() < 0 || request.getRequestId() > 1) {
                throw new IllegalArgumentException("requestId must be 0 or 1.");
            }
            return processUssd(request);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException(
                    "Invalid JSON body. Use commas between fields, not semicolons, and set Content-Type to application/json.",
                    ex);
        }
    }

    private UssdRequest parseUssdRequest(String body) throws JsonProcessingException {
        JsonNode payload = objectMapper.readTree(body);
        if (payload == null || payload.isNull()) {
            throw new IllegalArgumentException("USSD request body cannot be empty.");
        }
        if (payload.isTextual()) {
            payload = objectMapper.readTree(payload.asText());
        }
        if (payload == null || !payload.isObject()) {
            throw new IllegalArgumentException("USSD request must be a JSON object.");
        }
        return objectMapper.treeToValue(payload, UssdRequest.class);
    }

    private ResponseEntity<UssdResponse> processUssd(UssdRequest request) {
        try {
            ResponseEntity<UssdResponse> result = processUssdInternal(request);
            UssdResponse body = result.getBody();
            auditService.record(request,
                    body != null ? body.getSessionId() : null,
                    UssdDialLog.OUTCOME_OK,
                    body != null ? body.getMessage() : null,
                    null);
            return result;
        } catch (RuntimeException ex) {
            auditService.record(request, null, UssdDialLog.OUTCOME_FAILED, null, ex.getMessage());
            throw ex;
        }
    }

    private ResponseEntity<UssdResponse> processUssdInternal(UssdRequest request) {

        String phoneNumber = request.getPhoneNumber();
        String serviceCode = (request.getServiceCode() != null && !request.getServiceCode().isBlank())
                ? request.getServiceCode() : "*140#";
        String choice = request.getText() != null ? request.getText().trim() : "";

        List<Category> categories = orderedUssdCategories();

        if (request.getRequestId() == 1) {
            String forcedSessionId = (request.getSessionId() != null && !request.getSessionId().isBlank())
                    ? request.getSessionId() : null;
            return ResponseEntity.ok(startNewSession(phoneNumber, categories, forcedSessionId));
        }

        String sessionId = request.getSessionId();
        UssdSessionStore.Session session = null;

        if (sessionId != null && !sessionId.isBlank()) {
            session = sessionStore.get(sessionId);
        } else {
            UssdSessionRecord activeRecord = sessionRecordRepository
                    .findFirstByPhoneNumberAndStatusOrderByStartedAtDesc(
                            phoneNumber, UssdSessionRecord.STATUS_ACTIVE)
                    .orElse(null);
            if (activeRecord != null) {
                if (isExpired(activeRecord)) {
                    expireRecord(activeRecord);
                } else {
                    sessionId = activeRecord.getSessionId();
                    session = restoreSession(activeRecord);
                    sessionStore.put(sessionId, session);
                }
            }
        }

        if (sessionId == null || sessionId.isBlank()) {
            throw new InvalidSessionException(
                    "sessionId is required when requestId = 0. Copy the sessionId returned by requestId = 1.");
        }

        if (session == null) {
            UssdSessionRecord activeRecord = sessionRecordRepository.findBySessionId(sessionId)
                    .filter(record -> UssdSessionRecord.STATUS_ACTIVE.equals(record.getStatus()))
                    .orElse(null);
            if (activeRecord != null) {
                if (isExpired(activeRecord)) {
                    expireRecord(activeRecord);
                } else {
                    session = restoreSession(activeRecord);
                    sessionStore.put(sessionId, session);
                }
            }
        }

        if (session == null) {
            throw new InvalidSessionException(
                    "Session not found or has expired. Start a new session with requestId = 1.");
        }
        if (!phoneNumber.equals(session.getPhoneNumber())) {
            throw new InvalidSessionException("This session does not belong to the given phoneNumber.");
        }
        session.touch();

        String message;

        switch (session.getLevel()) {

            case LANGUAGE_SELECTION -> {
                message = handleLanguageSelection(sessionId, session, categories, choice);
                recordStep(sessionId, choice, UssdSessionStore.Level.LANGUAGE_SELECTION.name(), message);
                syncProgress(sessionId, phoneNumber, message);
                updateSessionRecord(sessionId, session, message);
                if (message.startsWith("END")) {
                    sessionStore.remove(sessionId);
                }
                return ResponseEntity.ok(UssdResponse.ok(sessionId, message));
            }

            case RESUME_DECISION -> {
                if (!"1".equals(choice) && !"2".equals(choice)) {
                    String endMessage = "END " + UssdMessages.get(session.getLanguage(),
                            UssdMessages.Key.INVALID_SELECTION, serviceCode);
                    session.setEndReason("INVALID_SELECTION");
                    endSession(sessionId, session, choice, endMessage);
                    return ResponseEntity.ok(UssdResponse.ok(sessionId, endMessage));
                }
                if ("2".equals(choice)) {
                    session.setEndReason("RESTARTED");
                    endSession(sessionId, session, choice, "END Starting a new session.");
                    progressStore.remove(phoneNumber);
                    return ResponseEntity.ok(startFreshSession(phoneNumber));
                }
                message = handleResumeDecision(sessionId, session, categories, choice, phoneNumber);
            }

            case CATEGORY_LIST -> message = handleCategoryLevel(sessionId, session, categories,
                    choice, serviceCode);

            default -> message = handleProductLevel(sessionId, session, categories, choice,
                    serviceCode);
        }

        recordStep(sessionId, choice, session.getLevel().name(), message);
        syncProgress(sessionId, phoneNumber, message);
        updateSessionRecord(sessionId, session, message);
        if (message.startsWith("END")) {
            sessionStore.remove(sessionId);
        }
        return ResponseEntity.ok(UssdResponse.ok(sessionId, message));
    }

    private UssdResponse startNewSession(String phoneNumber, List<Category> categories,
                                         String forcedSessionId) {
        closeOpenSessionsFor(phoneNumber, "SUPERSEDED");

        String sessionId = (forcedSessionId != null && !forcedSessionId.isBlank())
                ? forcedSessionId : sessionStore.newSessionId();

        UssdProgressStore.SavedProgress saved = progressStore.get(phoneNumber);
        if (saved != null) {
            UssdSessionStore.Session session =
                    new UssdSessionStore.Session(UssdSessionStore.Level.RESUME_DECISION, null, phoneNumber);
            session.setLanguage(saved.getLanguage() != null ? saved.getLanguage() : DEFAULT_LANGUAGE);
            sessionStore.put(sessionId, session);
            String prompt = buildResumePrompt(saved, categories);
            createSessionRecord(sessionId, session);
            recordStep(sessionId, "", UssdSessionStore.Level.RESUME_DECISION.name(), prompt);
            return UssdResponse.ok(sessionId, prompt);
        }

        return startFreshSession(phoneNumber, forcedSessionId);
    }

    private UssdResponse startFreshSession(String phoneNumber) {
        return startFreshSession(phoneNumber, null);
    }

    private UssdResponse startFreshSession(String phoneNumber, String forcedSessionId) {
        String sessionId = (forcedSessionId != null && !forcedSessionId.isBlank())
                ? forcedSessionId : sessionStore.newSessionId();

        UssdSessionStore.Session session =
                new UssdSessionStore.Session(UssdSessionStore.Level.CATEGORY_LIST, null, phoneNumber);
        session.setLanguage(DEFAULT_LANGUAGE);

        sessionStore.put(sessionId, session);
        createSessionRecord(sessionId, session);

        String welcome = buildCategoryPage(orderedUssdCategories(), 0, DEFAULT_LANGUAGE);
        recordStep(sessionId, "", UssdSessionStore.Level.CATEGORY_LIST.name(), welcome);
        return UssdResponse.ok(sessionId, welcome);
    }

    private void endSession(String sessionId, UssdSessionStore.Session session,
                            String choice, String message) {
        sessionStore.remove(sessionId);
        sessionRecordRepository.findBySessionId(sessionId).ifPresent(record -> {
            record.setStatus(UssdSessionRecord.STATUS_ENDED);
            record.setEndedAt(LocalDateTime.now());
            record.setLastActivityAt(LocalDateTime.now());
            record.setEndReason(session.getEndReason() != null ? session.getEndReason() : "COMPLETED");
            sessionRecordRepository.save(record);
        });
        recordStep(sessionId, choice, session.getLevel().name(), message);
    }

    private void createSessionRecord(String sessionId, UssdSessionStore.Session session) {
        UssdSessionRecord record = sessionRecordRepository.findBySessionId(sessionId)
                .orElse(new UssdSessionRecord());
        record.setSessionId(sessionId);
        record.setPhoneNumber(session.getPhoneNumber());
        record.setStatus(UssdSessionRecord.STATUS_ACTIVE);
        if (record.getStartedAt() == null) record.setStartedAt(LocalDateTime.now());
        record.setLastActivityAt(LocalDateTime.now());
        record.setCurrentLevel(session.getLevel().name());
        record.setCurrentPage(session.getPage());
        record.setLanguage(session.getLanguage() != null ? session.getLanguage().name() : DEFAULT_LANGUAGE.name());
        record.setEndedAt(null);
        record.setEndReason(null);
        sessionRecordRepository.save(record);
    }

    private void closeOpenSessionsFor(String phoneNumber, String reason) {
        for (UssdSessionRecord record : sessionRecordRepository
                .findByPhoneNumberAndStatus(phoneNumber, UssdSessionRecord.STATUS_ACTIVE)) {
            if (isExpired(record)) {
                expireRecord(record);
                continue;
            }
            record.setStatus(UssdSessionRecord.STATUS_ENDED);
            record.setEndedAt(LocalDateTime.now());
            record.setLastActivityAt(LocalDateTime.now());
            record.setEndReason(reason);
            sessionRecordRepository.save(record);
            sessionStore.remove(record.getSessionId());
            recordStep(record.getSessionId(), "",
                    record.getCurrentLevel() == null ? "UNKNOWN" : record.getCurrentLevel(),
                    "END Session closed: " + reason);
        }
    }

    private boolean isExpired(UssdSessionRecord record) {
        LocalDateTime last = record.getLastActivityAt() != null
                ? record.getLastActivityAt() : record.getStartedAt();
        return last == null
                || java.time.Duration.between(last, LocalDateTime.now()).compareTo(sessionStore.getTtl()) > 0;
    }

    private void expireRecord(UssdSessionRecord record) {
        record.setStatus(UssdSessionRecord.STATUS_ENDED);
        record.setEndedAt(LocalDateTime.now());
        record.setEndReason("TIMEOUT");
        sessionRecordRepository.save(record);
        sessionStore.remove(record.getSessionId());
        recordStep(record.getSessionId(), "",
                record.getCurrentLevel() == null ? "UNKNOWN" : record.getCurrentLevel(),
                "END Session timed out");
    }

    private void updateSessionRecord(String sessionId, UssdSessionStore.Session session, String message) {
        sessionRecordRepository.findBySessionId(sessionId).ifPresent(record -> {
            record.setLanguage(session.getLanguage() == null
                    ? DEFAULT_LANGUAGE.name() : session.getLanguage().name());
            record.setCurrentLevel(session.getLevel().name());
            record.setCategoryId(session.getCategoryId());
            record.setCurrentPage(session.getPage());
            record.setLastActivityAt(LocalDateTime.now());
            if (message.startsWith("END")) {
                record.setStatus(UssdSessionRecord.STATUS_ENDED);
                record.setEndedAt(LocalDateTime.now());
                record.setEndReason(session.getEndReason() != null ? session.getEndReason() : "COMPLETED");
            }
            sessionRecordRepository.save(record);
        });
    }

    private void recordStep(String sessionId, String input, String level, String response) {
        sessionStepRepository.save(new UssdSessionStep(sessionId, input, level, response));
    }

    private void syncProgress(String sessionId, String phoneNumber, String message) {
        UssdSessionStore.Session current = sessionStore.get(sessionId);
        if (current != null && message.startsWith("CON")) {
            progressStore.put(phoneNumber, new UssdProgressStore.SavedProgress(
                    current.getLevel(), current.getCategoryId(), current.getPage(), current.getLanguage()));
        } else {
            progressStore.remove(phoneNumber);
        }
    }

    private UssdSessionStore.Session restoreSession(UssdSessionRecord record) {
        UssdSessionStore.Level level;
        try {
            level = UssdSessionStore.Level.valueOf(record.getCurrentLevel());
        } catch (Exception ex) {
            level = UssdSessionStore.Level.CATEGORY_LIST;
        }
        UssdSessionStore.Session session =
                new UssdSessionStore.Session(level, record.getCategoryId(), record.getPhoneNumber());
        session.setPage(record.getCurrentPage() == null ? 0 : record.getCurrentPage());
        if (record.getLanguage() != null) {
            try {
                session.setLanguage(Language.valueOf(record.getLanguage()));
            } catch (IllegalArgumentException ignored) {
                session.setLanguage(DEFAULT_LANGUAGE);
            }
        } else {
            session.setLanguage(DEFAULT_LANGUAGE);
        }
        return session;
    }

    private String handleResumeDecision(String sessionId, UssdSessionStore.Session session,
                                        List<Category> categories, String choice, String phoneNumber) {
        if ("2".equals(choice)) {
            progressStore.remove(phoneNumber);
            session.setLevel(UssdSessionStore.Level.CATEGORY_LIST);
            session.setCategoryId(null);
            session.setPage(0);
            sessionStore.put(sessionId, session);
            return buildCategoryPage(categories, 0, session.getLanguage());
        }

        UssdProgressStore.SavedProgress saved = progressStore.get(phoneNumber);
        if (saved == null) {
            session.setLevel(UssdSessionStore.Level.CATEGORY_LIST);
            session.setCategoryId(null);
            session.setPage(0);
            sessionStore.put(sessionId, session);
            return buildCategoryPage(categories, 0, session.getLanguage());
        }

        session.setLevel(saved.getLevel());
        session.setCategoryId(saved.getCategoryId());
        session.setPage(saved.getPage());
        session.setLanguage(saved.getLanguage() != null ? saved.getLanguage() : DEFAULT_LANGUAGE);
        sessionStore.put(sessionId, session);

        if (saved.getLevel() == UssdSessionStore.Level.PRODUCT_LIST) {
            List<Product> products =
                    productRepository.findByCategoryIdAndParentIsNullOrderByIdAsc(saved.getCategoryId());
            session.setParentProductId(null);
            session.setProductPage(0);
            return buildProductMenu(products, 0, session.getLanguage());
        }
        return buildCategoryPage(categories, saved.getPage(), saved.getLanguage());
    }

    private String handleCategoryLevel(String sessionId, UssdSessionStore.Session session,
                                       List<Category> categories, String choice, String serviceCode) {
        int page = session.getPage();
        int pageStart = pageStart(page);
        int pageEnd = pageEnd(page, categories.size());
        boolean hasNext = pageEnd < categories.size();
        boolean hasPrev = page > 0;

        if ("n".equalsIgnoreCase(choice) && hasNext) {
            session.setPage(page + 1);
            sessionStore.put(sessionId, session);
            return buildCategoryPage(categories, page + 1, session.getLanguage());
        }

        if ("b".equalsIgnoreCase(choice) && hasPrev) {
            session.setPage(page - 1);
            sessionStore.put(sessionId, session);
            return buildCategoryPage(categories, page - 1, session.getLanguage());
        }

        if ("00".equals(choice)) {
            session.setEndReason("USER_EXIT");
            sessionStore.remove(sessionId);
            return "END " + UssdMessages.get(session.getLanguage(), UssdMessages.Key.EXIT_THANKS);
        }

        int menuNumber = parseIntOrDefault(choice, -1);
        if (menuNumber < pageStart || menuNumber >= pageEnd) {
            session.setEndReason("INVALID_SELECTION");
            sessionStore.remove(sessionId);
            return "END " + UssdMessages.get(session.getLanguage(),
                    UssdMessages.Key.INVALID_SELECTION, serviceCode);
        }

        Category selected = categories.get(menuNumber);

        if (CHANGE_LANGUAGE_KEY.equals(normalizeName(selected.getName()))) {
            session.setLevel(UssdSessionStore.Level.LANGUAGE_SELECTION);
            sessionStore.put(sessionId, session);
            return buildLanguagePrompt(session.getLanguage());
        }

        List<Product> products = productRepository
                .findByCategoryIdAndParentIsNullOrderByIdAsc(selected.getId());
        session.setLevel(UssdSessionStore.Level.PRODUCT_LIST);
        session.setCategoryId(selected.getId());
        session.setParentProductId(null);
        session.setProductPage(0);
        sessionStore.put(sessionId, session);
        return buildProductMenu(products, 0, session.getLanguage());
    }

    private String handleLanguageSelection(String sessionId, UssdSessionStore.Session session,
                                           List<Category> categories, String choice) {
        choice = choice == null ? "" : choice.trim();

        if ("1".equals(choice)) {
            session.setLanguage(Language.EN);
        } else if ("2".equals(choice)) {
            session.setLanguage(Language.RW);
        } else if ("00".equals(choice)) {
            session.setEndReason("USER_EXIT");
            sessionStore.remove(sessionId);
            return "END " + UssdMessages.get(session.getLanguage(), UssdMessages.Key.EXIT_THANKS);
        } else {
            return buildLanguagePrompt(session.getLanguage());
        }

        session.setLevel(UssdSessionStore.Level.CATEGORY_LIST);
        session.setPage(0);
        sessionStore.put(sessionId, session);
        return buildCategoryPage(categories, 0, session.getLanguage());
    }

    private String handleProductLevel(String sessionId, UssdSessionStore.Session session,
                                      List<Category> categories, String choice, String serviceCode) {
        if ("00".equals(choice)) {
            session.setEndReason("USER_EXIT");
            sessionStore.remove(sessionId);
            return "END " + UssdMessages.get(session.getLanguage(), UssdMessages.Key.EXIT_THANKS);
        }

        Long parentId = session.getParentProductId();
        List<Product> products = parentId == null
                ? productRepository.findByCategoryIdAndParentIsNullOrderByIdAsc(session.getCategoryId())
                : productRepository.findByParentIdOrderByIdAsc(parentId);
        int page = session.getProductPage();
        int start = page * PRODUCT_PAGE_SIZE;
        int end = Math.min(start + PRODUCT_PAGE_SIZE, products.size());

        if ("0".equals(choice)) {
            if (parentId != null) {
                session.setParentProductId(null);
                session.setProductPage(0);
                sessionStore.put(sessionId, session);
                return buildProductMenu(
                        productRepository.findByCategoryIdAndParentIsNullOrderByIdAsc(session.getCategoryId()),
                        0, session.getLanguage());
            }
            session.setLevel(UssdSessionStore.Level.CATEGORY_LIST);
            session.setCategoryId(null);
            sessionStore.put(sessionId, session);
            return buildCategoryPage(categories, session.getPage(), session.getLanguage());
        }

        if ("n".equalsIgnoreCase(choice) && end < products.size()) {
            session.setProductPage(page + 1);
            sessionStore.put(sessionId, session);
            return buildProductMenu(products, page + 1, session.getLanguage());
        }

        if ("b".equalsIgnoreCase(choice) && page > 0) {
            session.setProductPage(page - 1);
            sessionStore.put(sessionId, session);
            return buildProductMenu(products, page - 1, session.getLanguage());
        }

        int number = parseIntOrDefault(choice, -1);
        if (number < start + 1 || number > end) {
            session.setEndReason("INVALID_SELECTION");
            sessionStore.remove(sessionId);
            return "END " + UssdMessages.get(session.getLanguage(),
                    UssdMessages.Key.INVALID_SELECTION, serviceCode);
        }

        Product selected = products.get(number - 1);

        if (parentId == null) {
            List<Product> items = productRepository.findByParentIdOrderByIdAsc(selected.getId());
            if (!items.isEmpty()) {
                session.setParentProductId(selected.getId());
                session.setProductPage(0);
                sessionStore.put(sessionId, session);
                return buildProductMenu(items, 0, session.getLanguage());
            }
        }

        session.setEndReason("COMPLETED");
        sessionStore.remove(sessionId);
        return formatProductDetail(selected, session.getLanguage());
    }

    private String buildCategoryPage(List<Category> categories, int page, Language language) {
        if (categories.isEmpty()) {
            return "END " + UssdMessages.get(language, UssdMessages.Key.NO_CATEGORIES);
        }

        int start = pageStart(page);
        int end = pageEnd(page, categories.size());
        boolean hasNext = end < categories.size();
        boolean hasPrev = page > 0;

        StringBuilder sb = new StringBuilder("CON ")
                .append(UssdMessages.get(language, UssdMessages.Key.DIAL_HINT))
                .append("\n\n");
        for (int i = start; i < end; i++) {
            sb.append(i).append(") ")
                    .append(displayCategoryName(categories.get(i), language)).append("\n");
        }
        if (hasNext) {
            sb.append("n) ").append(UssdMessages.get(language, UssdMessages.Key.NEXT)).append("\n");
        }
        if (hasPrev) {
            sb.append("b) ").append(UssdMessages.get(language, UssdMessages.Key.BACK)).append("\n");
        }
        sb.append("00) ").append(UssdMessages.get(language, UssdMessages.Key.EXIT));
        return sb.toString();
    }

    private String buildLanguagePrompt(Language language) {
        Language display = language != null ? language : DEFAULT_LANGUAGE;
        return "CON " + UssdMessages.get(display, UssdMessages.Key.LANGUAGE_SELECTION) + "\n"
                + "1) " + UssdMessages.get(display, UssdMessages.Key.ENGLISH) + "\n"
                + "2) " + UssdMessages.get(display, UssdMessages.Key.KINYARWANDA) + "\n"
                + "00) " + UssdMessages.get(display, UssdMessages.Key.EXIT);
    }

    private String buildProductMenu(List<Product> products, int page, Language language) {
        if (products.isEmpty()) {
            return "END " + UssdMessages.get(language, UssdMessages.Key.NO_PRODUCTS);
        }

        int start = page * PRODUCT_PAGE_SIZE;
        int end = Math.min(start + PRODUCT_PAGE_SIZE, products.size());
        boolean hasNext = end < products.size();
        boolean hasPrev = page > 0;

        StringBuilder sb = new StringBuilder("CON ")
                .append(UssdMessages.get(language, UssdMessages.Key.DIAL_HINT))
                .append("\n\n");
        for (int i = start; i < end; i++) {
            Product p = products.get(i);
            String label = p.getDescription() != null && !p.getDescription().isBlank()
                    ? p.getName() + "=" + p.getDescription()
                    : p.getName();
            sb.append(i + 1).append(") ").append(label).append("\n");
        }
        if (hasNext) {
            sb.append("n) ").append(UssdMessages.get(language, UssdMessages.Key.NEXT)).append("\n");
        }
        if (hasPrev) {
            sb.append("b) ").append(UssdMessages.get(language, UssdMessages.Key.BACK)).append("\n");
        }
        sb.append("0) ").append(UssdMessages.get(language, UssdMessages.Key.RETURN_BACK));
        return sb.toString();
    }

    private String buildResumePrompt(UssdProgressStore.SavedProgress saved, List<Category> categories) {
        Language language = saved.getLanguage() != null ? saved.getLanguage() : DEFAULT_LANGUAGE;
        String where = "";
        if (saved.getLevel() == UssdSessionStore.Level.PRODUCT_LIST && saved.getCategoryId() != null) {
            where = categories.stream()
                    .filter(c -> c.getId().equals(saved.getCategoryId()))
                    .map(c -> " (" + displayCategoryName(c, language) + ")")
                    .findFirst()
                    .orElse("");
        }
        return "CON " + UssdMessages.get(language, UssdMessages.Key.RESUME_HEADER, where) + "\n"
                + "1) " + UssdMessages.get(language, UssdMessages.Key.RESUME_CONTINUE) + "\n"
                + "2) " + UssdMessages.get(language, UssdMessages.Key.RESUME_START_NEW) + "\n"
                + "00) " + UssdMessages.get(language, UssdMessages.Key.EXIT);
    }

    private String formatProductDetail(Product p, Language language) {
        String validity = p.getValidityDays() == null
                ? UssdMessages.get(language, UssdMessages.Key.VALIDITY_NO_EXPIRY)
                : p.getValidityDays() + " " + UssdMessages.get(language, UssdMessages.Key.DAYS);

        return "END " + p.getName() + "\n"
                + UssdMessages.get(language, UssdMessages.Key.LABEL_CATEGORY) + ": "
                + p.getCategory().getName() + "\n"
                + (p.getPrice() != null
                ? UssdMessages.get(language, UssdMessages.Key.LABEL_PRICE) + ": " + p.getPrice() + "\n"
                : "")
                + UssdMessages.get(language, UssdMessages.Key.LABEL_VALIDITY) + ": " + validity + "\n"
                + (p.getDescription() != null ? p.getDescription() : "");
    }

    private List<Category> orderedUssdCategories() {
        return categoryRepository.findAllByOrderByNameAsc().stream()
                .sorted(Comparator
                        .comparingInt((Category category) ->
                                CATEGORY_PRIORITY.getOrDefault(normalizeName(category.getName()),
                                        Integer.MAX_VALUE))
                        .thenComparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private String displayCategoryName(Category category, Language language) {
        if ("Ayo usigaranye".equalsIgnoreCase(category.getName())) {
            return language == Language.RW ? "Amafaranga asigaye" : "Balance";
        }
        if ("MTN Irekure Icyumweru".equalsIgnoreCase(category.getName())) {
            return language == Language.RW ? "MTN Irekure Icyumweru (iminsi 7)"
                    : "MTN Irekure Weekly (7 Days)";
        }
        if ("MTN Irekure Ukwezi".equalsIgnoreCase(category.getName())) {
            return language == Language.RW ? "MTN Irekure Ukwezi (iminsi 30)"
                    : "MTN Irekure Monthly (30 Days)";
        }
        if (CHANGE_LANGUAGE_KEY.equals(normalizeName(category.getName()))) {
            return UssdMessages.get(language, UssdMessages.Key.CHANGE_LANGUAGE);
        }
        return category.getName();
    }

    private int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }
}

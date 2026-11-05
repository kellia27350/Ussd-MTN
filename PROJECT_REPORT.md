# MTN Product Catalogue with USSD & Web Chatbot
## Project Report

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technology Stack](#2-technology-stack)
3. [System Architecture](#3-system-architecture)
4. [Database Design](#4-database-design)
5. [Application Flow — Activity Diagrams](#5-application-flow--activity-diagrams)
   - 5.1 [Full USSD Session Flow (*140#)](#51-full-ussd-session-flow-140)
   - 5.2 [USSD Payment Flow](#52-ussd-payment-flow)
   - 5.3 [USSD Session Resume (Dropped Call)](#53-ussd-session-resume-dropped-call)
   - 5.4 [Web Chat Widget Flow](#54-web-chat-widget-flow)
   - 5.5 [Session Cleanup Job Flow](#55-session-cleanup-job-flow)
6. [Module Descriptions](#6-module-descriptions)
7. [REST API Reference](#7-rest-api-reference)
8. [USSD Menu Screens (Both Languages)](#8-ussd-menu-screens-both-languages)
9. [Test Cases — Full USSD Coverage](#9-test-cases--full-ussd-coverage)
10. [Test Cases — Web Chat Coverage](#10-test-cases--web-chat-coverage)
11. [Test Cases — REST API Coverage](#11-test-cases--rest-api-coverage)
12. [Known Limitations and Future Improvements](#12-known-limitations-and-future-improvements)
13. [How to Run the Project](#13-how-to-run-the-project)

---

## 1. Project Overview

This project is a **Spring Boot application** that simulates the MTN Rwanda product catalogue, accessible through two different channels:

- **USSD Gateway** — A subscriber dials `*140#` from their mobile phone. The application acts as the USSD callback server, guiding the caller step-by-step through a text menu to browse bundles, pick a product, and pay for it using Airtime balance, MoMo (Mobile Money), or Ihereze (a borrow-now-repay-later loan service).
- **Web Chat Widget** — A browser-based chat interface embedded in a web page that mirrors the same product catalogue browsing experience but is adapted for a web audience (no phone number required, no USSD CON/END protocol, no payment flow).

The application is backed by a **MySQL database** (XAMPP) and uses **Spring Data JPA / Hibernate** to manage four tables: `categories`, `products`, `customer_accounts`, and `ussd_sessions`.

### What makes this interesting

The USSD flow is more than a simple menu tree. It handles:

- **Bilingual support** — English and Kinyarwanda, chosen by the caller at any time via `L` from the category menu
- **Session persistence** — Every dial-in is recorded in the database as an audit trail, completely separate from the in-memory live session state
- **Resume on dropped call** — If a caller's session is cut off mid-flow (call drops, phone dies), the next time they dial in they are offered the chance to continue exactly where they left off
- **Payment simulation** — Airtime balance deduction, MoMo PIN entry, and Ihereze (loan) with a 15% service fee that gets added to the caller's outstanding loan balance, repaid automatically on their next top-up

---

## 2. Technology Stack

| Layer | Technology | Version |
|---|---|---|
| Language | Java | 17 |
| Framework | Spring Boot | 3.3.4 |
| Web | Spring MVC (embedded Tomcat) | — |
| Persistence | Spring Data JPA + Hibernate | — |
| Database | MySQL (XAMPP) | 5.x / 8.x |
| Validation | Jakarta Bean Validation | — |
| XML Support | Jackson Dataformat XML | — |
| Build Tool | Maven | — |
| Testing | JUnit 5 + Spring Boot Test | — |

---

## 3. System Architecture

```
┌──────────────────────────────────────────────────────────────────────┐
│                          CLIENT LAYER                                │
│                                                                      │
│   ┌──────────────────────┐         ┌──────────────────────────────┐ │
│   │   Mobile Subscriber  │         │      Web Browser             │ │
│   │   Dials *140#        │         │      chat-widget.js          │ │
│   └──────────┬───────────┘         └─────────────┬────────────────┘ │
└──────────────┼──────────────────────────────────┼───────────────────┘
               │ POST /ussd                        │ POST /api/chat
               │ (JSON / XML / Form)               │ (JSON)
┌──────────────▼──────────────────────────────────▼───────────────────┐
│                        CONTROLLER LAYER                              │
│                                                                      │
│   ┌──────────────────────┐         ┌────────────────────────────┐   │
│   │   UssdController     │         │      ChatController        │   │
│   │   POST /ussd         │         │      POST /api/chat        │   │
│   │   GET /ussd/sessions │         └────────────────────────────┘   │
│   └──────────┬───────────┘                                          │
│              │                                                       │
│   ┌──────────────────────┐   ┌───────────────┐   ┌───────────────┐  │
│   │  CategoryController  │   │ProductController  │CustomerController│
│   │  /api/categories     │   │/api/products  │   │/api/customers │  │
│   └──────────────────────┘   └───────────────┘   └───────────────┘  │
└──────────────────────────────────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────────────────┐
│                         SERVICE LAYER                                │
│                                                                      │
│  ┌─────────────────────┐   ┌──────────────────────────────────────┐ │
│  │  CategoryService    │   │  UssdSessionStore (in-memory)        │ │
│  │  ProductService     │   │  ├─ keyed by sessionId (UUID)        │ │
│  └─────────────────────┘   │  └─ TTL: 3 minutes                  │ │
│                             └──────────────────────────────────────┘ │
│                             ┌──────────────────────────────────────┐ │
│                             │  UssdProgressStore (in-memory)       │ │
│                             │  ├─ keyed by phoneNumber             │ │
│                             │  └─ survives session boundaries      │ │
│                             └──────────────────────────────────────┘ │
│                             ┌──────────────────────────────────────┐ │
│                             │  ChatSessionStore (in-memory)        │ │
│                             │  ├─ keyed by sessionId (UUID)        │ │
│                             │  └─ TTL: 15 minutes                  │ │
│                             └──────────────────────────────────────┘ │
│                             ┌──────────────────────────────────────┐ │
│                             │  UssdSessionCleanupJob (@Scheduled)  │ │
│                             │  └─ runs every 60 seconds            │ │
│                             └──────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────────────────┐
│                      REPOSITORY LAYER (JPA)                          │
│                                                                      │
│   CategoryRepository  │  ProductRepository  │  CustomerAccountRepo  │
│   UssdSessionRecordRepository                                        │
└──────────────────────────────────────────────────────────────────────┘
               │
┌──────────────▼──────────────────────────────────────────────────────┐
│                     DATABASE (MySQL)                                 │
│                                                                      │
│   categories  │  products  │  customer_accounts  │  ussd_sessions   │
└──────────────────────────────────────────────────────────────────────┘
```

### Key design decisions

**Two separate session stores for USSD**

The project separates _live session state_ (`UssdSessionStore`, keyed by UUID sessionId) from _cross-session progress_ (`UssdProgressStore`, keyed by phone number). This is what makes the "resume where you left off" feature possible. A session ID is only valid for one continuous dial-in and expires after 3 minutes of inactivity. The phone-number-keyed progress snapshot survives indefinitely until either the subscriber completes a session (END reached) or they explicitly choose "Start a new session" on the resume prompt.

**No Lombok — everything hand-written**

All getters, setters, and constructors are written manually. This makes the code more verbose but removes any Lombok dependency and keeps each class fully readable without IDE plugins.

**Single processUssd() method regardless of Content-Type**

The USSD endpoint accepts JSON, XML, and form-encoded bodies. Rather than duplicating logic, both `handleUssdJsonOrXml()` and `handleUssdForm()` immediately delegate to the shared `processUssd()` method. The only difference between the two mapped methods is the binding annotation (`@RequestBody` vs `@ModelAttribute`).

---

## 4. Database Design

The application manages four tables. Hibernate creates and maintains them automatically via `ddl-auto=update`.

### 4.1 Entity-Relationship Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         categories                              │
├─────────────────┬────────────────┬──────────────────────────────┤
│ id              │ BIGINT         │ PK, AUTO_INCREMENT           │
│ name            │ VARCHAR(255)   │ NOT NULL, UNIQUE             │
│ description     │ VARCHAR(255)   │ NULL                         │
└────────┬────────┴────────────────┴──────────────────────────────┘
         │ 1
         │
         │ many
┌────────▼────────────────────────────────────────────────────────┐
│                          products                               │
├─────────────────┬────────────────┬──────────────────────────────┤
│ id              │ BIGINT         │ PK, AUTO_INCREMENT           │
│ name            │ VARCHAR(255)   │ NOT NULL                     │
│ description     │ VARCHAR(255)   │ NULL                         │
│ validity_days   │ INT            │ NULL (null = never expires)  │
│ price           │ DECIMAL(12,2)  │ NOT NULL, must be > 0        │
│ category_id     │ BIGINT         │ FK → categories.id, NOT NULL │
│ active          │ BOOLEAN        │ NOT NULL, DEFAULT TRUE       │
│ created_at      │ DATETIME       │ Set on insert, read-only     │
│ updated_at      │ DATETIME       │ Set on insert + update       │
└─────────────────┴────────────────┴──────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      customer_accounts                          │
├─────────────────┬────────────────┬──────────────────────────────┤
│ id              │ BIGINT         │ PK, AUTO_INCREMENT           │
│ phone_number    │ VARCHAR(255)   │ NOT NULL, UNIQUE             │
│ airtime_balance │ DECIMAL(12,2)  │ NOT NULL, DEFAULT 1000.00   │
│ outstanding_loan│ DECIMAL(12,2)  │ NOT NULL, DEFAULT 0.00       │
└─────────────────┴────────────────┴──────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                        ussd_sessions                            │
├─────────────────┬────────────────┬──────────────────────────────┤
│ id              │ BIGINT         │ PK, AUTO_INCREMENT           │
│ session_id      │ VARCHAR(255)   │ NOT NULL, UNIQUE (UUID)      │
│ phone_number    │ VARCHAR(255)   │ NOT NULL                     │
│ language        │ VARCHAR(10)    │ NULL until chosen (EN / RW)  │
│ status          │ VARCHAR(10)    │ NOT NULL (ACTIVE / ENDED)    │
│ started_at      │ DATETIME       │ NOT NULL                     │
│ ended_at        │ DATETIME       │ NULL until session ends      │
└─────────────────┴────────────────┴──────────────────────────────┘
```

### 4.2 Seeded Data (from DataSeeder.java)

On first startup the `DataSeeder` component inserts the following catalogue data. The seeding is transactional (all-or-nothing) and is skipped entirely if the `categories` table already has rows.

| Position | Category | Description | Product(s) | Price (Frw) | Validity |
|---|---|---|---|---|---|
| 0 | Gumamo | Serivisi yo kwibutsa / guma ku murongo | Gumamo | 1.00 | 1 day |
| 1 | MTN Irekure | MTN Irekure bundles (24hrs / weekly / monthly) | MTN Irekure(24hrs) | 100.00 | 1 day |
| 1 | MTN Irekure | (same category) | MTN Irekure Icyumweru | 500.00 | 7 days |
| 1 | MTN Irekure | (same category) | MTN Irekure Ukwezi | 1,500.00 | 30 days |
| 2 | Gwamon' | Iyi serivisi iracyakorwaho | Gwamon' | 1.00 | 1 day |
| 3 | Ayo usigaranye | Reba amafaranga asigaye kuri konti yawe | Ayo usigaranye | 1.00 | 1 day |

> **Why 3 products under one category?** The MTN Irekure bundles (24hrs, weekly, monthly) are grouped under a single category so the USSD product list presents them as a clean numbered sub-menu instead of cluttering the top-level category list.

### 4.3 Notes on Relationships

- `products.category_id` is a foreign key to `categories.id` with `ManyToOne` (EAGER fetch). Every product always belongs to exactly one category.
- `customer_accounts` has no foreign key relationship to any other table. Accounts are created lazily (on first USSD payment attempt or first `GET /api/customers/{phone}` call) and identified only by phone number.
- `ussd_sessions` has no foreign key to `customer_accounts`. It is a pure audit trail and is not joined in any query except by the cleanup job scanning for `status = ACTIVE`.

---

## 5. Application Flow — Activity Diagrams

The diagrams below use plain ASCII art and PlantUML-style pseudocode. Each step that sends a response to the caller/user is shown in **[brackets]**.

---

### 5.1 Full USSD Session Flow (*140#)

```
MOBILE SUBSCRIBER dials *140#
         │
         ▼
  ┌──────────────────────────────────────────────────────────┐
  │  POST /ussd  requestId = 1                               │
  │  phoneNumber = caller's MSISDN                           │
  └──────────────────────────────────┬───────────────────────┘
                                     │
                          ┌──────────▼──────────┐
                          │ Generate new UUID   │
                          │ sessionId           │
                          │ INSERT ussd_sessions│
                          │ status = ACTIVE     │
                          └──────────┬──────────┘
                                     │
                          ┌──────────▼──────────────────────┐
                          │ Check UssdProgressStore          │
                          │ Does phoneNumber have a saved    │
                          │ (abandoned) session?             │
                          └──────────┬──────────────────────┘
                                     │
                  ┌──────────────────┴────────────────────────┐
                 YES                                          NO
                  │                                           │
     ┌────────────▼──────────┐                  ┌────────────▼──────────────┐
     │ Level = RESUME_DECISION│                  │ Level = CATEGORY_LIST     │
     │ [CON Resume prompt]    │                  │ Language = EN             │
     │ 1. Continue            │                  │ [CON Category page 0]     │
     │ 2. Start new           │                  └────────────┬──────────────┘
     └────────────┬──────────┘                               │
                  │                                          ↓
    ┌─────────────┴──────────┐              ┌────────────────────────────────┐
    │ requestId=0 input=?    │              │ See CATEGORY_LIST step below   │
    └─────────┬──────────────┘              └────────────────────────────────┘
              │
   ┌──────────┴──────────────────────────────┐
  "1" Continue                             "2" New
   │                                          │
   ▼                                          ▼
  Restore level/page/                    Clear progressStore
  categoryId from progress               Level = CATEGORY_LIST
  Replay last screen                     Page = 0
                                         [CON Category page 0]

─────────────────────────────────────────────────────────────────────
CATEGORY_LIST  (Level = CATEGORY_LIST)
─────────────────────────────────────────────────────────────────────

     [CON screen example — English, page 0]:
     ┌────────────────────────────────────────┐
     │ Dial *100# for great deals!            │
     │ 0)Gumamo                               │
     │ 1)MTN Irekure                          │
     │ 2)Gwamon'                              │
     │ 3)Ayo usigaranye                       │
     │ L Change language                      │
     └────────────────────────────────────────┘

     requestId=0, user enters input
               │
    ┌──────────┴──────────────────────────────────────────┐
    │   What did the user input?                          │
    └──┬──────────┬──────────┬──────────┬────────────────┘
      "n"       "0"(pg>0)   "L"       digit(valid pos)   other
       │           │          │              │               │
   Next page  Prev page  Language       Select             END
   [CON]       [CON]     prompt         category          Invalid
                          [CON]             │              selection
                                            ▼
                                     Load products for
                                     selected category
                                     Level = PRODUCT_LIST
                                     Save categoryId
                                     Save progress snapshot
                                     [CON Product list]

─────────────────────────────────────────────────────────────────────
LANGUAGE_SELECTION  (Level = LANGUAGE_SELECTION)
─────────────────────────────────────────────────────────────────────

     [CON screen]:
     ┌────────────────────────────────────────┐
     │ Choose language / Hitamo ururimi:      │
     │ 1. English                             │
     │ 2. Kinyarwanda                         │
     └────────────────────────────────────────┘

     input?
       │
    ┌──┴────────────────────────┐
   "1"                         "2"                  other
    │                           │                     │
  EN                          RW                  END bilingual
  Update DB record           Update DB record      error, endSession
  Level = CATEGORY_LIST      Level = CATEGORY_LIST
  Page = 0                   Page = 0
  [CON Category page 0]      [CON Category page 0]

─────────────────────────────────────────────────────────────────────
PRODUCT_LIST  (Level = PRODUCT_LIST)
─────────────────────────────────────────────────────────────────────

     [CON screen example — MTN Irekure]:
     ┌────────────────────────────────────────────────────────┐
     │ MTN Irekure bundles (24hrs / weekly / monthly)         │
     │ 1)100Frw= MTN Irekure(24hrs)                           │
     │ 2)500Frw= MTN Irekure Icyumweru                        │
     │ 3)1500Frw= MTN Irekure Ukwezi                          │
     │ 0)Return Back                                          │
     └────────────────────────────────────────────────────────┘

     input?
       │
    ┌──┴──────────────────────────────────────────────────────┐
   "0"                   digit(1..n valid)                  other
    │                           │                              │
  Level =               Set pendingProductId               END
  CATEGORY_LIST         Level = PAYMENT_METHOD             Invalid
  [CON Category page]   [CON Payment menu]                 selection
```

---

### 5.2 USSD Payment Flow

```
─────────────────────────────────────────────────────────────────────
PAYMENT_METHOD  (Level = PAYMENT_METHOD)
─────────────────────────────────────────────────────────────────────

     [CON screen]:
     ┌────────────────────────────────────────────────────────────┐
     │ Pay 100Frw = MTN Irekure(24hrs) via:                       │
     │ 1)Airtime                                                  │
     │ 2)Momo                                                     │
     │ 3)Ihereze                                                  │
     │ 0)Return Back                                              │
     └────────────────────────────────────────────────────────────┘

     input?
       │
    ┌──┴──────────────────────────────────────────────────────────┐
   "0"          "1" Airtime       "2" Momo        "3" Ihereze   other
    │               │                 │                 │           │
  Back to      Check account     Level =           Level =       END
  PRODUCT_LIST balance           MOMO_PIN          LOAN_CONFIRM  Invalid
                   │             [CON PIN]         [CON Confirm]
          ┌────────┴─────────┐
      balance ≥ price    balance < price
          │                    │
     Deduct balance       Level = LOAN_OFFER
     Save account         [CON Insufficient
     endSession()          balance notice]
     [END Product detail
      + "Paid via Airtime"]

─────────────────────────────────────────────────────────────────────
MOMO_PIN  (Level = MOMO_PIN)
─────────────────────────────────────────────────────────────────────

     [CON screen]:
     ┌────────────────────────────────────────┐
     │ Enter your Momo PIN:                   │
     └────────────────────────────────────────┘

     input?
       │
    ┌──┴─────────────────────────────────────┐
  any non-blank input                   blank / empty
    │                                         │
  (Simulated — no real MoMo API)          endSession()
  endSession()                            [END Invalid selection]
  [END Product detail + "Paid via Momo"]

─────────────────────────────────────────────────────────────────────
LOAN_OFFER  (Level = LOAN_OFFER — insufficient Airtime balance)
─────────────────────────────────────────────────────────────────────

     Fee = price × 15%
     Total = price + fee

     [CON screen]:
     ┌──────────────────────────────────────────────────────────────┐
     │ You have insufficient balance, confirm to get a loan of      │
     │ 100Frw with 15Frw service fee. You will repay 115Frw on      │
     │ your next recharge:                                          │
     │ n Next                                                       │
     └──────────────────────────────────────────────────────────────┘

     input?
       │
    ┌──┴─────────────────────────────┐
   "n"                            anything else
    │                                  │
  Level = LOAN_CONFIRM             endSession()
  [CON Loan confirm screen]        [END Invalid selection]

─────────────────────────────────────────────────────────────────────
LOAN_CONFIRM  (Level = LOAN_CONFIRM)
─────────────────────────────────────────────────────────────────────

     [CON screen]:
     ┌──────────────────────────────────────────────────────────────┐
     │ Confirm to get a loan of 100Frw with 15Frw service fee.      │
     │ You will repay 115Frw on your next recharge:                 │
     │ 1)Confirm                                                    │
     │ 2)Cancel                                                     │
     │ 0)Go back                                                    │
     └──────────────────────────────────────────────────────────────┘

     input?
       │
    ┌──┴────────────────────────────────────────────────────────────┐
   "1" Confirm         "2" Cancel           "0" Go back         other
    │                      │                     │                  │
  Add total to         endSession()          Level =            endSession()
  outstandingLoan      [END "Purchase        PAYMENT_METHOD     [END Invalid]
  Save account          cancelled."]        [CON Payment menu]
  endSession()
  [END Product detail
   + "Loan approved via
     Ihereze. Repay 115Frw
     on next recharge."]
```

---

### 5.3 USSD Session Resume (Dropped Call)

```
  Subscriber is browsing MTN Irekure products
  Call drops unexpectedly (no END response ever sent)
                │
                ▼
  UssdProgressStore still holds:
  { level: PRODUCT_LIST, categoryId: 2, page: 0, language: EN }
                │
  (UssdSessionStore entry for that sessionId expires after 3 min)
                │
  UssdSessionCleanupJob runs (every 60 sec):
     - Finds ACTIVE DB record for that sessionId
     - sessionStore.get() returns null (expired)
     - Updates DB: status = ENDED, endedAt = now
                │
                ▼
  Subscriber dials *140# again (requestId = 1, NEW sessionId)
                │
                ▼
  startNewSession() checks progressStore.get(phoneNumber)
                │
     ┌──────────▼──────────────────────────┐
     │ Saved progress found!               │
     │ Language from saved progress = EN   │
     │ Level = RESUME_DECISION             │
     │ INSERT new ussd_sessions row        │
     └──────────┬──────────────────────────┘
                │
     [CON "You have an unfinished session (MTN Irekure).
           1. Continue where you left off
           2. Start a new session"]
                │
         Caller presses "1"
                │
         Restore level=PRODUCT_LIST, categoryId=2, page=0
                │
     [CON MTN Irekure product list — exactly where they were]
```

---

### 5.4 Web Chat Widget Flow

```
  Browser user visits the page
  chat-widget.js loads
             │
             ▼
  POST /api/chat  { sessionId: null, message: "" }
             │
  Server generates sessionId, Level = CATEGORY_LIST
             │
  [Response: { sessionId: "...", ended: false,
               message: "Hi! 👋 What would you like to browse?
                          0. Gumamo
                          1. MTN Irekure
                          2. Gwamon'
                          3. Ayo usigaranye" }]
             │
  User types a number (e.g. "1")
             │
  POST /api/chat  { sessionId: "...", message: "1" }
             │
  Level = CATEGORY_LIST, choice = "1"
  Load products for MTN Irekure
  Level → PRODUCT_LIST, save categoryId
             │
  [Response: { ended: false,
               message: "MTN Irekure:
                          1. MTN Irekure(24hrs) - 100.00
                          2. MTN Irekure Icyumweru - 500.00
                          3. MTN Irekure Ukwezi - 1500.00
                          0. Back
                          00. Exit" }]
             │
  User types "2"
             │
  Level = PRODUCT_LIST, productIndex = 2
  Select products.get(1) = MTN Irekure Icyumweru
  Remove session (chat ends after product detail)
             │
  [Response: { ended: true,
               message: "MTN Irekure Icyumweru
                          Category: MTN Irekure
                          Price: 500.00
                          Validity: 7 day(s)
                          Irekure bundle valid for 1 week" }]

─────────────────────────────────────────────────────────────────────
Navigation shortcuts:
  "n"  → next page of categories (if more exist)
  "0"  → previous page (if on page > 0) OR back to categories from product list
  "00" → exit immediately from product list ("Thanks for stopping by! 👋")
─────────────────────────────────────────────────────────────────────
```

---

### 5.5 Session Cleanup Job Flow

```
  @Scheduled every 60 seconds
         │
         ▼
  Load all UssdSessionRecord where status = ACTIVE
         │
         ▼
  For each active record:
         │
    ┌────▼─────────────────────────────────────────┐
    │ sessionStore.get(record.getSessionId())      │
    │  → internally checks lastAccessedAt          │
    │  → if > 3 min ago: evicts + returns null     │
    └────┬─────────────────────────────────────────┘
         │
    ┌────┴───────────────────────┐
  null (expired)           non-null (still alive)
    │                            │
  Update DB record:          Do nothing
  status = ENDED             (leave it running)
  endedAt = now
  Save
```

---

## 6. Module Descriptions

### 6.1 Models

| Class | Table | Purpose |
|---|---|---|
| `Category` | `categories` | A product group (e.g. MTN Irekure). Has a one-to-many relationship with products. |
| `Product` | `products` | A single purchasable item. Belongs to exactly one category. Has a price (BigDecimal), optional validity in days, and an active flag. |
| `CustomerAccount` | `customer_accounts` | Simulates a subscriber's wallet. Tracks Airtime balance (starts at 1000 Frw) and outstanding loan (starts at 0). Created lazily on first access. |
| `UssdSessionRecord` | `ussd_sessions` | Durable audit trail of every USSD dial-in. One row per session. Records start time, end time, language chosen, and status (ACTIVE/ENDED). |
| `Language` | — (enum) | `EN` (English) or `RW` (Kinyarwanda). Used to look up the right string in `UssdMessages`. |

### 6.2 Services

| Class | Type | Purpose |
|---|---|---|
| `CategoryService` | Spring Service | CRUD operations for categories. Enforces unique name constraint. |
| `ProductService` | Spring Service | CRUD + search operations for products. Supports filtering by category, name, and active status. |
| `UssdSessionStore` | Spring Component (in-memory) | Tracks the live state (current Level, page, categoryId, language, pendingProductId) of an active USSD session. Keyed by UUID sessionId. Sessions auto-expire after 3 minutes. |
| `UssdProgressStore` | Spring Component (in-memory) | Persists a caller's last known position across session boundaries. Keyed by phone number. Enables "resume where you left off" after a dropped call. |
| `ChatSessionStore` | Spring Component (in-memory) | Same role as UssdSessionStore but for the web chat widget. TTL is 15 minutes. Simpler — no payment levels, no resume, no language. |
| `UssdSessionCleanupJob` | Spring Component (@Scheduled) | Runs every 60 seconds. Closes any DB session records that were never explicitly ended (dropped calls). |

### 6.3 Controllers

| Class | Base Path | Purpose |
|---|---|---|
| `CategoryController` | `/api/categories` | Full REST CRUD for categories. |
| `ProductController` | `/api/products` | Full REST CRUD for products with optional query filters. |
| `CustomerController` | `/api/customers` | View and top-up a customer's simulated wallet. |
| `UssdController` | `/ussd` | Core of the application. Handles all USSD dial-in logic including session management, language, browse, payment, and loan flows. |
| `ChatController` | `/api/chat` | Web chat widget backend. Mirrors the USSD browse experience without payment or language switching. |

### 6.4 Utilities

| Class | Purpose |
|---|---|
| `UssdMessages` | Static lookup table for all user-visible strings in English and Kinyarwanda. 32 message keys. Accepts `String.format` args for dynamic values. |
| `DataSeeder` | Seeds 4 categories and 6 products on first startup. Transactional, idempotent (skips if data already exists). |
| `GlobalExceptionHandler` | `@RestControllerAdvice` that maps exceptions to HTTP responses (404, 400, 415, 500). |

---

## 7. REST API Reference

All endpoints return JSON unless otherwise noted.

### Categories

| Method | URL | Request Body | Response | Description |
|---|---|---|---|---|
| GET | `/api/categories` | — | `[{id, name, description}]` 200 | List all categories |
| GET | `/api/categories/{id}` | — | `{id, name, description}` 200 / 404 | Get one category |
| POST | `/api/categories` | `{name*, description}` | `{id, name, description}` 201 / 400 | Create category |
| PUT | `/api/categories/{id}` | `{name*, description}` | `{id, name, description}` 200 / 404 | Update category |
| DELETE | `/api/categories/{id}` | — | — 204 / 404 | Delete category |

### Products

| Method | URL | Query Params | Request Body | Response | Description |
|---|---|---|---|---|---|
| GET | `/api/products` | `categoryId`, `name`, `activeOnly` | — | `[…]` 200 | List/filter products |
| GET | `/api/products/{id}` | — | — | `{…}` 200 / 404 | Get one product |
| POST | `/api/products` | — | `{name*, price*, categoryId*, …}` | `{…}` 201 | Create product |
| PUT | `/api/products/{id}` | — | `{name*, price*, categoryId*, …}` | `{…}` 200 | Update product |
| DELETE | `/api/products/{id}` | — | — | — 204 | Delete product |

### Customers

| Method | URL | Request Body | Response | Description |
|---|---|---|---|---|
| GET | `/api/customers/{phoneNumber}` | — | `{id, phoneNumber, airtimeBalance, outstandingLoan}` 200 | Get (or create) account |
| POST | `/api/customers/{phoneNumber}/topup` | `{amount*}` | Updated account 200 | Top up airtime; repays loan first |

### USSD

| Method | URL | Content-Types | Description |
|---|---|---|---|
| POST | `/ussd` | `application/json`, `application/xml`, `application/x-www-form-urlencoded` | Main USSD gateway callback |
| GET | `/ussd/sessions` | — | List all recorded USSD sessions |

### Chat

| Method | URL | Request Body | Response |
|---|---|---|---|
| POST | `/api/chat` | `{sessionId, message}` | `{sessionId, message, ended}` |

---

## 8. USSD Menu Screens (Both Languages)

### Category List — English

```
Dial *100# for great deals!
0)Gumamo
1)MTN Irekure
2)Gwamon'
3)Ayo usigaranye
L Change language
```

### Category List — Kinyarwanda

```
Hamagara *100# uhabwe ibiciro byiza!
0)Gumamo
1)MTN Irekure
2)Gwamon'
3)Ayo usigaranye
L Hindura ururimi
```

### Product List — MTN Irekure (English)

```
MTN Irekure bundles (24hrs / weekly / monthly)
1)100Frw= MTN Irekure(24hrs)
2)500Frw= MTN Irekure Icyumweru
3)1500Frw= MTN Irekure Ukwezi
0)Return Back
```

### Payment Method Menu — English

```
Pay 100Frw = MTN Irekure(24hrs) via:
1)Airtime
2)Momo
3)Ihereze
0)Return Back
```

### Insufficient Balance Notice — English

```
You have insufficient balance, confirm to get a loan of 100Frw
with 15Frw service fee. You will repay 115Frw on your next recharge:
n Next
```

### Loan Confirm — English

```
Confirm to get a loan of 100Frw with 15Frw service fee.
You will repay 115Frw on your next recharge:
1)Confirm
2)Cancel
0)Go back
```

### MoMo PIN Prompt — Kinyarwanda

```
Andika ijambo ry'ibanga (PIN) rya Momo:
```

### Resume Prompt — English

```
You have an unfinished session (MTN Irekure).
1. Continue where you left off
2. Start a new session
```

### Product Detail — English (END screen)

```
END MTN Irekure(24hrs)
Category: MTN Irekure
Price: 100.00
Validity: 1 day(s)
Irekure bundle valid for 24 hours
Paid via Airtime
```

---

## 9. Test Cases — Full USSD Coverage

The test cases below are written as request/response pairs. The endpoint is
`POST http://localhost:8081/ussd` with `Content-Type: application/json`.

---

### TC-01: Start a new session — first-time caller

**Request:**
```json
{
  "requestId": 1,
  "phoneNumber": "+250780000001",
  "text": ""
}
```

**Expected Response:**
```json
{
  "sessionId": "<generated-uuid>",
  "message": "CON Dial *100# for great deals!\n0)Gumamo\n1)MTN Irekure\n2)Gwamon'\n3)Ayo usigaranye\nL Change language",
  "error": false
}
```

**What is being verified:** A brand-new session is created. A row is inserted into `ussd_sessions` with `status = ACTIVE`. The caller sees the category list directly (no resume prompt because they have no saved progress).

---

### TC-02: Select a category — MTN Irekure (position 1)

**Prerequisites:** TC-01 completed. Save the `sessionId` from that response.

**Request:**
```json
{
  "requestId": 0,
  "sessionId": "<session-id-from-TC-01>",
  "phoneNumber": "+250780000001",
  "text": "1"
}
```

**Expected Response:**
```json
{
  "sessionId": "<same-session-id>",
  "message": "CON MTN Irekure bundles (24hrs / weekly / monthly)\n1)100Frw= MTN Irekure(24hrs)\n2)500Frw= MTN Irekure Icyumweru\n3)1500Frw= MTN Irekure Ukwezi\n0)Return Back",
  "error": false
}
```

**What is being verified:** Category selection transitions to `PRODUCT_LIST`. Products are listed with 1-based numbering and price formatted without trailing zeros.

---

### TC-03: Go back from product list to category list

**Prerequisites:** TC-02 completed.

**Request:**
```json
{
  "requestId": 0,
  "sessionId": "<same-session-id>",
  "phoneNumber": "+250780000001",
  "text": "0"
}
```

**Expected Response:** Category list is shown again (same page the user was on).

**What is being verified:** "0" at `PRODUCT_LIST` level returns to `CATEGORY_LIST` at the same page (page 0 in this case).

---

### TC-04: Select a product — MTN Irekure(24hrs)

**Prerequisites:** TC-02 completed (user is on MTN Irekure product list).

**Request:**
```json
{
  "requestId": 0,
  "sessionId": "<same-session-id>",
  "phoneNumber": "+250780000001",
  "text": "1"
}
```

**Expected Response — Payment menu:**
```json
{
  "message": "CON Pay 100Frw = MTN Irekure(24hrs) via:\n1)Airtime\n2)Momo\n3)Ihereze\n0)Return Back",
  "error": false
}
```

**What is being verified:** Selecting product 1 sets `pendingProductId` and transitions session to `PAYMENT_METHOD`.

---

### TC-05: Pay with Airtime — sufficient balance

**Prerequisites:** TC-04 completed. The caller (+250780000001) has a fresh account with 1000.00 Frw balance (product costs 100.00 Frw).

**Request:**
```json
{
  "requestId": 0,
  "sessionId": "<same-session-id>",
  "phoneNumber": "+250780000001",
  "text": "1"
}
```

**Expected Response:**
```json
{
  "message": "END MTN Irekure(24hrs)\nCategory: MTN Irekure\nPrice: 100.00\nValidity: 1 day(s)\nIrekure bundle valid for 24 hours\nPaid via Airtime",
  "error": false
}
```

**What is being verified:** Balance is deducted. Session is ended in both `UssdSessionStore` and `ussd_sessions` DB. `progressStore` is cleared for this phone number.

**Post-condition check:**
```
GET /api/customers/+250780000001
→ { airtimeBalance: 900.00, outstandingLoan: 0.00 }
```

---

### TC-06: Pay with Airtime — insufficient balance (triggers loan flow)

**Prerequisites:** Customer account has a balance lower than the product price.

**Setup:** Manually set balance to 50.00 Frw (either via a PUT or by using a phone number that hasn't been used before and then running a test that deplete the balance, or by adjusting directly in the DB for testing purposes).

**Request (pick product, then select Airtime):**
```json
{ "requestId": 0, "sessionId": "<session-id>", "phoneNumber": "+250780000050", "text": "1" }
```
(Repeat category→product selection first, then send this as the Airtime choice at PAYMENT_METHOD.)

**Expected Response — Loan offer:**
```json
{
  "message": "CON You have insufficient balance, confirm to get a loan of 100Frw with 15Frw service fee. You will repay 115Frw on your next recharge:\nn Next",
  "error": false
}
```

**What is being verified:** When balance < product price, the session does NOT end but instead transitions to `LOAN_OFFER`. The fee is correctly computed as 100 × 0.15 = 15.00, total = 115.00.

---

### TC-07: Advance through Loan Offer with "n"

**Prerequisites:** TC-06 completed.

**Request:**
```json
{ "requestId": 0, "sessionId": "<same>", "phoneNumber": "+250780000050", "text": "n" }
```

**Expected Response — Loan confirm screen:**
```json
{
  "message": "CON Confirm to get a loan of 100Frw with 15Frw service fee. You will repay 115Frw on your next recharge:\n1)Confirm\n2)Cancel\n0)Go back",
  "error": false
}
```

**What is being verified:** Only "n" advances from `LOAN_OFFER`. Session transitions to `LOAN_CONFIRM`.

---

### TC-08: Confirm the loan

**Prerequisites:** TC-07 completed.

**Request:**
```json
{ "requestId": 0, "sessionId": "<same>", "phoneNumber": "+250780000050", "text": "1" }
```

**Expected Response:**
```json
{
  "message": "END MTN Irekure(24hrs)\nCategory: MTN Irekure\nLoan of 100Frw approved via Ihereze. Repay 115Frw on your next recharge.",
  "error": false
}
```

**What is being verified:** `outstandingLoan` is increased by 115.00. Session ends. DB record is updated to ENDED.

**Post-condition:**
```
GET /api/customers/+250780000050
→ { airtimeBalance: 50.00, outstandingLoan: 115.00 }
```

---

### TC-09: Cancel the loan

**Prerequisites:** Back at `LOAN_CONFIRM` screen (repeat TC-06 and TC-07 with a different phone/session).

**Request:**
```json
{ "requestId": 0, "sessionId": "<session>", "phoneNumber": "+250780000051", "text": "2" }
```

**Expected Response:**
```json
{
  "message": "END Purchase cancelled.",
  "error": false
}
```

**What is being verified:** "2" at `LOAN_CONFIRM` ends the session with a cancellation message. No loan is recorded.

---

### TC-10: Go back from loan confirm to payment menu

**Prerequisites:** At `LOAN_CONFIRM` screen.

**Request:**
```json
{ "requestId": 0, "sessionId": "<session>", "phoneNumber": "+250780000052", "text": "0" }
```

**Expected Response:** Payment method menu is shown again.

**What is being verified:** "0" at `LOAN_CONFIRM` restores `PAYMENT_METHOD` level.

---

### TC-11: Pay with Momo

**Prerequisites:** At `PAYMENT_METHOD` screen for any product.

**Request (select Momo):**
```json
{ "requestId": 0, "sessionId": "<session>", "phoneNumber": "+250780000002", "text": "2" }
```

**Expected Response:**
```json
{
  "message": "CON Enter your Momo PIN:",
  "error": false
}
```

**Follow-up request (enter PIN):**
```json
{ "requestId": 0, "sessionId": "<session>", "phoneNumber": "+250780000002", "text": "1234" }
```

**Expected Response:**
```json
{
  "message": "END MTN Irekure(24hrs)\nCategory: MTN Irekure\nPrice: 100.00\nValidity: 1 day(s)\nIrekure bundle valid for 24 hours\nPaid via Momo",
  "error": false
}
```

**What is being verified:** Any non-blank PIN is accepted (simulation). Session ends successfully.

---

### TC-12: Pay with Ihereze (direct, not via insufficient balance)

**Prerequisites:** At `PAYMENT_METHOD` screen.

**Request:**
```json
{ "requestId": 0, "sessionId": "<session>", "phoneNumber": "+250780000003", "text": "3" }
```

**Expected Response — Loan confirm screen directly:**
```json
{
  "message": "CON Confirm to get a loan of 100Frw with 15Frw service fee. You will repay 115Frw on your next recharge:\n1)Confirm\n2)Cancel\n0)Go back"
}
```

**What is being verified:** Ihereze option skips `LOAN_OFFER` and goes straight to `LOAN_CONFIRM`.

---

### TC-13: Switch language to Kinyarwanda

**Prerequisites:** At `CATEGORY_LIST` in English.

**Request:**
```json
{ "requestId": 0, "sessionId": "<session>", "phoneNumber": "+250780000004", "text": "L" }
```

**Expected Response — Language prompt:**
```json
{
  "message": "CON Choose language / Hitamo ururimi:\n1. English\n2. Kinyarwanda"
}
```

**Follow-up (select Kinyarwanda):**
```json
{ "requestId": 0, "sessionId": "<session>", "phoneNumber": "+250780000004", "text": "2" }
```

**Expected Response — Category list in Kinyarwanda:**
```json
{
  "message": "CON Hamagara *100# uhabwe ibiciro byiza!\n0)Gumamo\n1)MTN Irekure\n2)Gwamon'\n3)Ayo usigaranye\nL Hindura ururimi"
}
```

**What is being verified:** Language switch works. Category list and all subsequent screens use Kinyarwanda strings. DB record `language` column is updated to "RW".

---

### TC-14: Resume a dropped session

**Prerequisites:**
1. Start a session for +250780000005 (requestId=1)
2. Navigate to the MTN Irekure product list (two more requests)
3. Do NOT send any further requests — let the session expire (wait 3+ minutes, or just proceed — the test verifies the behavior on next requestId=1)

**New session request (requestId=1):**
```json
{ "requestId": 1, "phoneNumber": "+250780000005", "text": "" }
```

**Expected Response — Resume prompt:**
```json
{
  "message": "CON You have an unfinished session (MTN Irekure).\n1. Continue where you left off\n2. Start a new session"
}
```

**Follow-up (continue):**
```json
{ "requestId": 0, "sessionId": "<new-session-id>", "phoneNumber": "+250780000005", "text": "1" }
```

**Expected Response:** MTN Irekure product list is shown — exactly where the user was.

**What is being verified:** `progressStore` survives session expiry. Choosing "Continue" restores the exact level and categoryId.

---

### TC-15: Invalid session ID (requestId=0 with unknown sessionId)

**Request:**
```json
{ "requestId": 0, "sessionId": "not-a-real-session-id", "phoneNumber": "+250780000006", "text": "1" }
```

**Expected Response:** HTTP 400

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Session not found or has expired. Please start a new session (requestId = 1)."
}
```

**What is being verified:** `InvalidSessionException` is thrown and handled by `GlobalExceptionHandler`. The endpoint does not crash.

---

### TC-16: Missing sessionId when requestId=0

**Request:**
```json
{ "requestId": 0, "phoneNumber": "+250780000007", "text": "1" }
```

**Expected Response:** HTTP 400 with message "sessionId is required when requestId = 0."

---

### TC-17: phoneNumber mismatch

**Prerequisites:** Start a session for +250780000008, save its sessionId.

**Request (wrong phone number):**
```json
{ "requestId": 0, "sessionId": "<session-id-for-08>", "phoneNumber": "+250780000999", "text": "1" }
```

**Expected Response:** HTTP 400 — "This session does not belong to the given phoneNumber."

---

### TC-18: Invalid category selection

**Prerequisites:** At `CATEGORY_LIST` (page 0 — items 0–3 are valid).

**Request:**
```json
{ "requestId": 0, "sessionId": "<session>", "phoneNumber": "+250780000009", "text": "99" }
```

**Expected Response:**
```json
{
  "message": "END Invalid selection. Please dial *140# again."
}
```

**What is being verified:** Out-of-range selection ends the session with a clear error. The session is marked ENDED in the DB.

---

### TC-19: Invalid product selection

**Prerequisites:** At `PRODUCT_LIST` for MTN Irekure (3 products, so valid choices are 1–3).

**Request:**
```json
{ "requestId": 0, "sessionId": "<session>", "phoneNumber": "+250780000010", "text": "9" }
```

**Expected Response:** END with "Invalid selection."

---

### TC-20: Loan offer refuses anything other than "n"

**Prerequisites:** At `LOAN_OFFER` screen.

**Request:**
```json
{ "requestId": 0, "sessionId": "<session>", "phoneNumber": "+250780000011", "text": "1" }
```

**Expected Response:** END with "Invalid selection."

**What is being verified:** `LOAN_OFFER` only accepts "n". Any other input ends the session.

---

### TC-21: Top-up repays outstanding loan first

**Prerequisites:** Customer +250780000050 has `outstandingLoan = 115.00`, `airtimeBalance = 50.00` (result of TC-08).

**Request:**
```
POST /api/customers/+250780000050/topup
Content-Type: application/json

{ "amount": 200.00 }
```

**Expected Response:**
```json
{
  "phoneNumber": "+250780000050",
  "airtimeBalance": 135.00,
  "outstandingLoan": 0.00
}
```

**Calculation:** Top-up = 200.00. Outstanding loan = 115.00. After repayment: loan = 0, remaining = 85.00. New balance = 50.00 + 85.00 = 135.00.

**What is being verified:** Loan is repaid first; only the remainder goes to Airtime balance.

---

### TC-22: Form-urlencoded USSD request

**Request:**
```
POST /ussd
Content-Type: application/x-www-form-urlencoded

requestId=1&phoneNumber=%2B250780000020&text=
```

**Expected Response:** Same category list as TC-01 — JSON response (since the client's Accept header defaults to JSON).

**What is being verified:** The `handleUssdForm()` method and `@ModelAttribute` binding work correctly for gateway integrations that send form-encoded data.

---

### TC-23: XML USSD request and XML response

**Request:**
```
POST /ussd
Content-Type: application/xml
Accept: application/xml

<ussdRequest>
  <requestId>1</requestId>
  <phoneNumber>+250780000021</phoneNumber>
  <text></text>
  <serviceCode>*140#</serviceCode>
</ussdRequest>
```

**Expected Response (XML):**
```xml
<ussdResponse>
  <sessionId>…</sessionId>
  <message>CON Dial *100# for great deals!…</message>
  <error>false</error>
</ussdResponse>
```

**What is being verified:** `jackson-dataformat-xml` correctly serializes and deserializes USSD requests and responses in XML format.

---

### TC-24: requestId out of range

**Request:**
```json
{ "requestId": 5, "phoneNumber": "+250780000022", "text": "" }
```

**Expected Response:** HTTP 400 (Bean Validation `@Max(1)` fails).

---

### TC-25: Missing phoneNumber

**Request:**
```json
{ "requestId": 1, "text": "" }
```

**Expected Response:** HTTP 400 with validation error on `phoneNumber` field.

---

### TC-26: Session cleanup — DB audit trail after dropped call

**Manual / automated integration test steps:**

1. Start a session (requestId=1) for +250780000030. Note the sessionId.
2. Verify `ussd_sessions` has a row with `status = ACTIVE`.
3. Wait 4 minutes (longer than the 3-minute SESSION_TTL) without sending any further requests.
4. After the `UssdSessionCleanupJob` runs (at the next 60-second tick after minute 3), query the DB.

**Expected DB state:**
```sql
SELECT status, ended_at FROM ussd_sessions WHERE session_id = '<sessionId>';
-- status = 'ENDED', ended_at IS NOT NULL
```

**What is being verified:** The cleanup job correctly transitions expired ACTIVE records to ENDED, preventing zombie rows in the audit table.

---

### TC-27: Category list — Kinyarwanda throughout the full flow

**Steps:**

1. Start session (requestId=1) → see English category list.
2. Send "L" → see language prompt.
3. Send "2" → now in Kinyarwanda. Category list in Kinyarwanda.
4. Send "1" → MTN Irekure product list. Header and "Return Back" label in Kinyarwanda.
5. Send "1" → Payment method menu in Kinyarwanda.
6. Send "3" (Ihereze) → Loan confirm in Kinyarwanda.
7. Send "1" → Product detail + loan approval note in Kinyarwanda.

**What is being verified:** The entire flow renders in the correct language from start to finish, including all dynamic values (product name, price, fee, total).

---

## 10. Test Cases — Web Chat Coverage

### TC-CHAT-01: Start a new chat

**Request:**
```
POST /api/chat
Content-Type: application/json

{ "sessionId": null, "message": "" }
```

**Expected Response:**
```json
{
  "sessionId": "<generated-uuid>",
  "message": "Hi! 👋 What would you like to browse?\n0. Gumamo\n1. MTN Irekure\n2. Gwamon'\n3. Ayo usigaranye",
  "ended": false
}
```

---

### TC-CHAT-02: Pick a category

```json
{ "sessionId": "<from above>", "message": "1" }
```

**Expected Response:**
```json
{
  "message": "MTN Irekure:\n1. MTN Irekure(24hrs) - 100.00\n2. MTN Irekure Icyumweru - 500.00\n3. MTN Irekure Ukwezi - 1500.00\n0. Back\n00. Exit",
  "ended": false
}
```

---

### TC-CHAT-03: Pick a product and see detail

```json
{ "sessionId": "<from above>", "message": "2" }
```

**Expected Response:**
```json
{
  "message": "MTN Irekure Icyumweru\nCategory: MTN Irekure\nPrice: 500.00\nValidity: 7 day(s)\nIrekure bundle valid for 1 week",
  "ended": true
}
```

---

### TC-CHAT-04: Exit with "00" from product list

```json
{ "sessionId": "<session-at-product-list>", "message": "00" }
```

**Expected:**
```json
{ "message": "Thanks for stopping by! 👋", "ended": true }
```

---

### TC-CHAT-05: Go back to categories with "0"

```json
{ "sessionId": "<session-at-product-list>", "message": "0" }
```

**Expected:** Category list at the same page, `ended: false`.

---

### TC-CHAT-06: Navigate next category page

Requires more than 5 categories in DB.

```json
{ "sessionId": "<session-at-category-list>", "message": "n" }
```

**Expected:** Next page of categories with `ended: false`. If no next page exists, "n" is treated as an invalid selection and the session ends with an error.

---

### TC-CHAT-07: Re-use an expired sessionId starts a new chat

```json
{ "sessionId": "completely-unknown-id", "message": "1" }
```

**Expected:** A brand-new session is created (new sessionId), and the response is the initial category list. No error is thrown — the chat widget simply re-starts rather than rejecting the user.

---

### TC-CHAT-08: Invalid category selection

```json
{ "sessionId": "<session-at-category-list>", "message": "99" }
```

**Expected:**
```json
{
  "message": "Sorry, that's not a valid selection. Start a new chat and try again.",
  "ended": true
}
```

---

## 11. Test Cases — REST API Coverage

### TC-API-01: Create a category

```
POST /api/categories
{ "name": "MTN Data", "description": "Mobile data bundles" }
→ 201 { "id": 5, "name": "MTN Data", "description": "Mobile data bundles" }
```

---

### TC-API-02: Create duplicate category name

```
POST /api/categories
{ "name": "Gumamo" }
→ 400 "A category named 'Gumamo' already exists."
```

---

### TC-API-03: Create a product

```
POST /api/products
{
  "name": "Daily Data 100MB",
  "description": "100MB valid for 1 day",
  "validityDays": 1,
  "price": 50.00,
  "categoryId": 5
}
→ 201 { "id": 7, "name": "Daily Data 100MB", "price": 50.00, ... }
```

---

### TC-API-04: Filter products by category

```
GET /api/products?categoryId=1
→ 200 [ 3 Irekure products ]
```

---

### TC-API-05: Search products by name

```
GET /api/products?name=irekure
→ 200 [ all products with "irekure" in the name (case-insensitive) ]
```

---

### TC-API-06: Get active products only

```
GET /api/products?activeOnly=true
→ 200 [ all products where active = true ]
```

---

### TC-API-07: Delete a product

```
DELETE /api/products/7
→ 204 No Content
```

---

### TC-API-08: Get non-existent resource

```
GET /api/categories/999
→ 404 { "status": 404, "error": "Not Found", "message": "Category not found with id: 999" }
```

---

### TC-API-09: View all USSD sessions

```
GET /ussd/sessions
→ 200 [ all rows from ussd_sessions table as JSON ]
```

---

### TC-API-10: Create product with invalid price (zero)

```
POST /api/products
{ "name": "Free product", "price": 0, "categoryId": 1 }
→ 400 validation error on price field
```

---

### TC-API-11: Create product with non-existent category

```
POST /api/products
{ "name": "Test", "price": 10, "categoryId": 9999 }
→ 404 "Category not found with id: 9999"
```

---

## 12. Known Limitations and Future Improvements

### Current limitations

| Area | Limitation |
|---|---|
| In-memory stores | `UssdSessionStore`, `UssdProgressStore`, and `ChatSessionStore` are stored in `ConcurrentHashMap`. They are lost on application restart and cannot be shared across multiple app instances. |
| MoMo integration | The MoMo PIN flow is purely simulated — any non-blank input is accepted as a valid PIN. No real MTN MoMo API is called. |
| Airtime balance | Customer accounts are seeded with a fixed 1,000 Frw balance. There is no integration with actual telecom billing. |
| Authentication | There is no security layer. All API endpoints are publicly accessible. |
| Pagination — chat | The chat widget uses 0-based absolute indexing for categories, which means position "0" picks the first category AND is also the "previous page" signal on pages > 0, creating ambiguity. |
| Single test | The only automated test is a Spring context load test. No unit or integration tests exist for business logic. |

### Suggested improvements

- Move session stores to **Redis** to support horizontal scaling and persistence across restarts.
- Add **Spring Security** with JWT authentication to protect the admin REST endpoints.
- Replace in-memory progress tracking with a **DB-backed `ussd_progress` table** for true crash resilience.
- Integrate the real **MTN MoMo API** for payment processing.
- Write **proper unit and integration tests** using JUnit 5 and MockMvc for all service and controller logic.
- Consider **pagination** for the `/api/categories` and `/api/products` endpoints to handle large catalogues.
- Add **request logging** (Spring Boot Actuator + MDC) to trace each USSD session through the log output.

---

## 13. How to Run the Project

### Prerequisites

- Java 17 or later
- Maven 3.6+
- XAMPP with MySQL running on port 3306 (default, no password for root)

### Steps

```bash
# 1. Clone the project
git clone <repo-url>
cd product-catalogue

# 2. Start MySQL (via XAMPP Control Panel or service)
# The database "product_catalogue" is created automatically on first run.

# 3. Build the project
mvn clean install

# 4. Run the application
mvn spring-boot:run
# OR
java -jar target/product-catalogue-1.0.0.jar
```

The application starts on **http://localhost:8081**.

### Quick smoke test

```
# 1. Start a USSD session
curl -X POST http://localhost:8081/ussd \
  -H "Content-Type: application/json" \
  -d '{"requestId":1,"phoneNumber":"+250780000001","text":""}'

# 2. Browse to the category list
# (Use the sessionId from the response above)

# 3. View all recorded sessions
curl http://localhost:8081/ussd/sessions

# 4. Open the chat widget
# Navigate to http://localhost:8081 in your browser
```

---

*Report generated from source code analysis of the `product-catalogue` Spring Boot project.*
*All flows, screen contents, database schemas, and test cases are derived directly from the implementation.*

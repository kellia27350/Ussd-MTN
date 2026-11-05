# Product Catalogue API + USSD (*140#)

A Spring Boot REST API for managing a product catalogue (airtime, data, voice, SMS bundles), plus a USSD menu interface reachable at **\*140#**.

## Tech Stack
- Java 17+
- Spring Boot 3.5
- PostgreSQL 18
- Maven

---

## Database Setup

1. Make sure PostgreSQL is running on your machine
2. The app connects automatically — no manual table creation needed (Hibernate handles it)

### Connection details (`application.properties`)
- Host: `localhost:5432`
- Database: `product_catalogue`
- Username: `postgres`
- Password: `123`

To change these, edit `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/product_catalogue
spring.datasource.username=postgres
spring.datasource.password=123
```

---

## How to Run

### VS Code
1. Install **Spring Boot Extension Pack** by VMware
2. Open the project folder in VS Code
3. Open the terminal with `` Ctrl+` `` and run:
```powershell
mvn spring-boot:run
```

### IntelliJ IDEA
Open `ProductCatalogueApplication.java` and click the green ▶ run button.

The app starts on **http://localhost:8082**

---

## REST Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/categories` | List all categories |
| GET | `/api/categories/{id}` | Get one category |
| POST | `/api/categories` | Create category |
| PUT | `/api/categories/{id}` | Update category |
| DELETE | `/api/categories/{id}` | Delete category |
| GET | `/api/products` | List all products |
| GET | `/api/products?categoryId=1` | Filter by category |
| GET | `/api/products?name=data` | Search by name |
| GET | `/api/products/{id}` | Get one product |
| POST | `/api/products` | Create product |
| PUT | `/api/products/{id}` | Update product |
| DELETE | `/api/products/{id}` | Delete product |

---

## USSD Endpoint — `POST /ussd`

### JSON format
```json
{
  "requestId": 1,
  "phoneNumber": "+250780000000",
  "text": ""
}
```

| Field | Description |
|-------|-------------|
| `requestId` | `1` = new session, `0` = continue |
| `sessionId` | Required when `requestId = 0` |
| `phoneNumber` | Caller's number |
| `text` | What the user pressed (`""`, `"0"`, `"1"`, `"n"`, `"00"`) |

### MTN XML format — `POST /ussd/mtn`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<request type="pull">
    <msisdn>250789355454</msisdn>
    <imsi></imsi>
    <input>0</input>
    <sessionid>174653269315987</sessionid>
    <CellID>37886</CellID>
    <new_request>1</new_request>
    <parameters></parameters>
    <freeflow>
        <mode>FD</mode>
    </freeflow>
</request>
```

| Field | Description |
|-------|-------------|
| `new_request` | `1` = new session, `0` = continue |
| `input` | What the user pressed |
| `sessionid` | Keep the same throughout one session |

---

## Session Audit Endpoints

| Endpoint | Description |
|----------|-------------|
| `GET /api/ussd/sessions` | All sessions |
| `GET /api/ussd/sessions/{sessionId}/steps` | Step-by-step history of a session |
| `GET /api/ussd/dial-logs` | Every request ever received |
| `GET /api/ussd/callers` | Summary per phone number |

---

## Testing

Import `product-catalogue-ussd.postman_collection.json` into Postman — all requests are pre-built.

### Quick USSD test flow
1. `POST /ussd` with `requestId: 1` → get `sessionId` + see main menu
2. `POST /ussd` with `requestId: 0` + `sessionId` + `text: "0"` → My Deal products
3. `POST /ussd` with `requestId: 0` + `sessionId` + `text: "1"` → product detail

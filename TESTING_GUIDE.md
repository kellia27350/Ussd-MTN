# Run & test the Product Catalogue (USSD) with PostgreSQL + Postman in VS Code

## 1. Prerequisites
- JDK 17+ (`java -version`)
- Maven (`mvn -v`) – or install the VS Code "Extension Pack for Java" + "Spring Boot Extension Pack"
- PostgreSQL running on port 5432 (pgAdmin or psql)
- Postman

## 2. Create the database (once)
```sql
CREATE DATABASE product_catalogue;
```
`application.properties` already points to it (user `postgres`, password `123` – change if yours differs).
Tables are created/updated automatically (`ddl-auto=update`), including the new ones:
`ussd_dial_logs` (new) and the new `end_reason` column of `ussd_sessions`.

## 3. Run
Open the `product-catalogue` folder in IntelliJ (File > Open > pom.xml > Open as Project) and run `ProductCatalogueApplication`, or in a terminal:
```bash
mvn spring-boot:run
```
App listens on http://localhost:8081. Seed data (categories/products) is inserted on first start.

## 4. Test in Postman
1. Import `product-catalogue-ussd.postman_collection.json`.
2. Run folders in order (right-click folder → Run). Variables: `baseUrl`, `phone`, `sessionId` (saved automatically).
3. Folder **F** shows what was recorded.

## 5. What is recorded now
| Table | One row per | Purpose |
|---|---|---|
| `ussd_dial_logs` | every request to `/ussd` (OK **and** failed) | every phone number that ever dialled |
| `ussd_sessions` | session | status `ACTIVE`→`ENDED`, `ended_at`, `end_reason` |
| `ussd_session_steps` | every action inside a session | input typed + response shown |

`end_reason` values: `COMPLETED` (product shown), `USER_EXIT` (00), `INVALID_SELECTION`, `TIMEOUT` (3 min silence, closed by the 1-minute sweep), `SUPERSEDED` (user dialled again while an old session was open), `RESTARTED` (chose "start new" at resume prompt).

Once a session is `ENDED` its sessionId is dead (reusing it returns 400) and the next dial gets a fresh one – nothing blocks later recording.

## 6. Useful SQL checks (pgAdmin Query Tool)
```sql
-- every number that dialled, how many times
SELECT phone_number, COUNT(*) dials, MIN(created_at) first_seen, MAX(created_at) last_seen
FROM ussd_dial_logs GROUP BY phone_number ORDER BY last_seen DESC;

-- sessions and how they ended
SELECT session_id, phone_number, status, end_reason, started_at, ended_at FROM ussd_sessions ORDER BY started_at DESC;

-- no zombie sessions should remain after 3-4 minutes of silence
SELECT * FROM ussd_sessions WHERE status = 'ACTIVE';

-- timeline of one session
SELECT created_at, level, input_text, response_message FROM ussd_session_steps WHERE session_id = '<id>' ORDER BY created_at;
```

## 7. Read-only API endpoints added
`GET /api/ussd/callers` · `/api/ussd/dial-logs[?phoneNumber=]` · `/api/ussd/sessions[?phoneNumber=&status=]` · `/api/ussd/sessions/{sessionId}/steps`

## 8. Troubleshooting
- `password authentication failed` → fix `spring.datasource.password`.
- `database "product_catalogue" does not exist` → run step 2.
- Port 8081 busy → change `server.port`.
- Postman 415 → body must be raw **JSON** with `Content-Type: application/json`.

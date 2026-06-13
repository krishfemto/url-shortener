# URL Shortener Service

A production-style URL shortener (like bit.ly) built with **Java 21 + Spring Boot**,
featuring **Redis caching**, **async click tracking**, and a **scheduled cleanup job**
for expired links.

---

## ✨ Features

- Shorten any long URL into a 7-character code (Base62)
- Fast redirects using Redis cache (cache-first, DB fallback)
- Click analytics per short URL
- Async click-count updates (doesn't slow down redirects)
- Automatic daily cleanup of expired URLs
- Input validation (rejects invalid URLs)
- Proper error handling (404 for not found, 410 for expired)
- Unit tests with Mockito (no DB/Redis required to run tests)

---

## 🏗️ Architecture

```
                 ┌─────────────┐
   POST /api/shorten ──────────▶│ Controller  │
                 └──────┬──────┘
                        │
                        ▼
                 ┌─────────────┐      save mapping      ┌────────────┐
                 │   Service   │────────────────────────▶│ PostgreSQL │
                 └──────┬──────┘                         └────────────┘
                        │ pre-warm cache
                        ▼
                 ┌─────────────┐
                 │    Redis    │  (24h TTL)
                 └─────────────┘


   GET /{shortCode} ──────────▶  Controller ──▶ Service
                                                    │
                              ┌─────────────────────┴─────────────────────┐
                              │ 1. Check Redis (cache)                     │
                              │    ├─ HIT  → return URL immediately        │
                              │    └─ MISS → query PostgreSQL, re-cache    │
                              │ 2. Increment click count ASYNC (background)│
                              └─────────────────────────────────────────────┘

   Daily @ 2 AM ──▶ UrlCleanupJob ──▶ delete expired rows from DB + Redis
```

---

## 🛠️ Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Database | PostgreSQL 16 |
| Cache | Redis 7 |
| ORM | Spring Data JPA (Hibernate) |
| Build | Maven |
| Testing | JUnit 5 + Mockito |
| Containerization | Docker Compose |

---

## 🚀 How to Run

### 1. Start PostgreSQL + Redis
```bash
docker-compose up -d
```

### 2. Run the application
```bash
mvn spring-boot:run
```

The app starts on `http://localhost:8080`.

### 3. Run tests
```bash
mvn test
```

---

## 📡 API Examples

### Create a short URL
```bash
curl -X POST http://localhost:8080/api/shorten \
  -H "Content-Type: application/json" \
  -d '{"originalUrl": "https://www.fggcorporation.co.jp/careers", "expiryDays": 30}'
```

**Response:**
```json
{
  "shortCode": "aB3xY9z",
  "shortUrl": "http://localhost:8080/aB3xY9z",
  "originalUrl": "https://www.fggcorporation.co.jp/careers",
  "expiresAt": "2026-07-13T10:00:00"
}
```

### Visit the short URL (redirects)
```bash
curl -L http://localhost:8080/aB3xY9z
```

### View analytics
```bash
curl http://localhost:8080/api/analytics/aB3xY9z
```

**Response:**
```json
{
  "shortCode": "aB3xY9z",
  "originalUrl": "https://www.fggcorporation.co.jp/careers",
  "totalClicks": 3,
  "createdAt": "2026-06-13T10:00:00",
  "expiresAt": "2026-07-13T10:00:00",
  "expired": false
}
```

---

## 💡 Key Design Decisions

**Why Redis cache-first?**
Redirects need to be fast. Reading from Redis (~1ms) is much faster than
PostgreSQL (~10-50ms). On a cache miss, we fall back to the database and
re-populate the cache for next time.

**Why async click tracking?**
Incrementing a click counter on every single redirect would add database
write latency to every request. By marking `incrementClickAsync()` with
`@Async`, the redirect response is sent immediately while the click count
updates in the background.

**Why an atomic UPDATE for click count instead of read-modify-write?**
`UPDATE url_mappings SET click_count = click_count + 1` runs entirely in the
database and is safe under concurrent traffic. A read-then-write approach
(`count = getCount(); setCount(count + 1); save();`) can lose updates if two
requests run at the same time (race condition).

**Why a scheduled cleanup job?**
Expired URLs would otherwise accumulate forever in the database. The daily
job removes them from both PostgreSQL and Redis, keeping the dataset clean.

---

## 📂 Project Structure

```
src/main/java/com/krishna/urlshortener/
├── UrlShortenerApplication.java   (entry point)
├── entity/UrlMapping.java         (database table)
├── repository/                    (database access)
├── service/
│   ├── UrlShortenerService.java   (core business logic)
│   └── UrlCleanupJob.java         (scheduled cleanup)
├── controller/                    (REST API endpoints)
├── dto/                           (request/response objects)
├── exception/                     (custom exceptions + handler)
└── config/RedisConfig.java        (Redis setup)
```

---

## 👤 Author

Krishnaraj Ramachandran — [GitHub](https://github.com/krishfemto)

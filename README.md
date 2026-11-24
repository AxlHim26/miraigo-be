# MiraiGo Backend

MiraiGo Backend is the core API platform for MiraiGo, a Japanese learning system with structured courses, practice workflows, and account management.

## Core Domains

- Authentication and authorization (JWT-based)
- Course hierarchy management:
  - Course
  - Chapter
  - Section
  - Lesson
- Vocabulary and grammar data services
- User progress and review support

## Technology

- **Java 21**
- **Spring Boot 3.2.5**
- **Spring Security**
- **Spring Data JPA (PostgreSQL)**
- **Spring Data Redis**
- **Hibernate** schema management (`ddl-auto`)
- **JLPT mock exams** auto-seeded from `mock_tests/*.json` on startup
- **Micrometer + Prometheus** metrics
- **Maven**
- **Docker Compose** deployment

## Architecture

```text
src/main/java/com/example/japanweb/
  config/          # Security, caching, observability, app config
  controller/      # REST controllers
  dto/             # Request/response models
  entity/          # JPA entities
  repository/      # Data access layer
  service/         # Business logic contracts + implementations
  security/        # JWT filters and auth utilities
  exception/       # Error model and global handlers
```

## Observability and Performance

- SQL query metrics exported via Actuator / Prometheus
- Slow-query tracking support
- Redis-backed operational components
- Repository-level optimizations for course tree loading and list retrieval

## Brand

All platform naming and project-facing documentation now uses **MiraiGo** branding.

## Vocabulary sets

Authenticated clients use `/api/v1/vocab/sets`. The default list contains
sets owned by the signed-in user. Add `?community=true` to list shared sets.
Only the owner can change a set or its words. The copy endpoint
`POST /api/v1/vocab/sets/{id}/copy` saves a private copy and its words in
one transaction.

For databases where schema updates are managed manually, apply
`V32__add_owned_vocabulary_sets.sql` after the existing schema. With
Hibernate `ddl-auto=update`, the new tables are created when the API starts.

To prepare the community sets from the previous FE JSON, generate SQL and
review it before applying to the target database:

```sh
python3 scripts/export_legacy_vocabulary_sql.py ../miraigo-fe/data/vocabulary-db.json > vocabulary-import.sql
psql -v ON_ERROR_STOP=1 -f vocabulary-import.sql
```

The generator imports shared sets by default. For private sets with verified
owners, pass `--owners owners.json` with a JSON object mapping legacy set IDs
to existing account emails. Existing set IDs are skipped, so applying the
same import again preserves any later edits. Keep the original JSON as a
backup for private sets with no confirmed owner.

## Kanji and practice tools

- `GET /api/v1/kanji/{glyph}/map` returns related characters, radicals,
  shared components and a stroke order animation when available. Regenerate
  the catalog with
  `python3 scripts/export_kanji_details.py ../miraigo-fe/data/kanji > src/main/resources/kanji/map.json`.
- `POST /api/v1/kanji/handwriting` accepts up to 40 strokes in a 300 × 300
  drawing area. Each stroke is a list of points with `x` and `y`. The API
  sends only the drawing to the same recognition provider used by the FE.
- `POST /api/v1/practice/chat` returns an event stream. Each text event
  contains a JSON string, followed by `[DONE]` on success. The reading
  endpoints under `/api/v1/practice/reading` generate, annotate and explain text.

These routes require a MiraiGo access token. Set `PRACTICE_ENGINE_URL` to the
trusted origin hosting the existing Next.js practice API. Run that service
with its Groq credentials. If FE runs on the same machine as Docker Desktop
or Colima, use `http://host.docker.internal:3000` where that hostname resolves.
The Voice AI service is configured separately in the mobile app.

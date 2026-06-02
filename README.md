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

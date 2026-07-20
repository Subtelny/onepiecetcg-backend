# One Piece TCG Backend - Rules for AI-Assisted Development

Spring Boot 4.1.0 / Java 21 REST backend for a One Piece TCG app. Consumed by a React frontend at `~/WebstormProjects/onepiecetcg`. This document is a **rulebook**, not a tutorial — read the referenced files for concrete patterns before writing code.

---

## 1. Project Context

- **Stack:** Java 21, Spring Boot 4.1.0 (WebMvc, Validation, Data JPA), JOOQ (complex/read-heavy queries only, see §3), SpringDoc OpenAPI 3.0, Lombok, PostgreSQL (Docker Compose).
- **Server:** port `3000`, base path `/api`, CORS enabled for `http://localhost:5173`.
- **Persistence status:**
  - Card sets and set/promo cards → PostgreSQL via Spring Data JPA.
  - Filter option values → PostgreSQL via Spring Data JPA, one row per distinct filterable value — a precomputed cache, not user-facing data.
  - Decks and shops → in-memory, not yet migrated to persistent storage.
- **External sync:** independent scheduled jobs pull from `https://www.optcgapi.com/api` (configurable via an externalized base-url property) and write into Postgres: card sets; set cards (a single job that fetches both regular set cards and promo cards from OPTCG and combines them). See §4 for the extension pattern they follow.
- Swagger UI: `http://localhost:3000/swagger-ui.html`. OpenAPI spec: `http://localhost:3000/api-docs`.
- Reference key endpoints: `GET/POST/PUT/DELETE /api/decks`, `GET /api/decks/featured`, `GET/POST /api/shops`, `GET /api/cards`, `GET /api/cards/{id}`, `GET /api/home/*`, `/api/tournaments/*`.

---

## 2. Architecture Rules

Hexagonal architecture (Ports & Adapters) with DDD-flavored naming. Package layout:

```
pl.janda.onepiecetcg/
├── application/        # Core — framework-independent (except @Service/@Transactional)
│   ├── model/           # Domain POJOs (JPA-annotated only for persisted entities)
│   ├── repository/      # Repository interfaces (ports)
│   ├── client/          # Outbound API client interfaces (ports)
│   └── service/         # Business logic
├── infrastructure/      # Adapters
│   ├── persistence/     # Repository implementations (Jpa*/InMemory*)
│   ├── client/           # HTTP client implementations (Optcg*)
│   └── scheduler/        # @Scheduled / @EventListener entrypoints
└── web/                 # HTTP adapters
    ├── controller/
    ├── dto/
    └── mapper/
```

**Dependency direction (enforced, no exceptions):**
```
web ──► application ◄── infrastructure
application ──X──► web            (forbidden)
application ──X──► infrastructure  (forbidden)
infrastructure ──X──► web          (forbidden)
```

---

## 3. Hexagonal Rules (Ports & Adapters)

- `application/repository/*` and `application/client/*` are **ports** — interfaces only, no implementation, no framework types beyond return values (`Optional<T>`, `List<T>`).
- `infrastructure/persistence/*` and `infrastructure/client/*` are **adapters** — implement the ports, contain all framework/HTTP/SQL-specific code.
- One port method per external capability — do not bundle unrelated operations into one method.
- Schedulers (`infrastructure/scheduler/*`) and controllers (`web/controller/*`) are **entrypoints**: they call `application/service/*`, never call adapters directly.
- Derived-query methods with no custom filtering logic are declared directly on the JPA repository interface (Spring Data generates them) — no need for a manual implementation.
- Complex/read-heavy queries (multi-field dynamic search/filtering, cross-row aggregation, bulk recompute) are implemented with **JOOQ** (`DSLContext`) in a dedicated class in `infrastructure/persistence/` (e.g. `Jooq{Noun}QueryAdapter`), called from the corresponding `Jpa{Noun}Repository` method — pushes filtering, sorting, pagination (`LIMIT`/`OFFSET`), counting, and grouping to the database instead of loading full tables into memory and processing them with Java Streams. This superseded the earlier `findAll()` + Streams default-method convention for these cases; simple derived queries with no such logic still don't need it.
- JOOQ generated sources (jOOQ codegen, target `generated-sources`), `DSLContext`, and JOOQ record/table types are adapter-only — they must never appear in `application/repository/*` port signatures or in `application/service/*`; ports keep returning plain domain types (`Optional<T>`, `List<T>`).
- If a JPA-backed adapter method (e.g. `saveAll`) is followed, within the same transaction, by a JOOQ query reading/writing the same table (raw JDBC via `DSLContext`, bypassing the Hibernate session), that JPA adapter method must flush the persistence context first (e.g. `saveAllAndFlush` instead of `saveAll`) — otherwise the JOOQ query can see stale or incomplete data, since Hibernate cannot auto-flush ahead of non-Hibernate-session queries.
- Postgres-native generated columns that jOOQ codegen doesn't model as a plain typed field (e.g. `set_cards.card_text_search_vector`, `set_cards.card_semantic_search_vector` — both `GENERATED ALWAYS ... STORED` tsvector columns backing DESCRIPTION/BOTH and SEMANTIC full-text search respectively) are never added to the JPA entity — they're written/read only via raw JOOQ SQL escape hatches (`field()`/`condition()` templates) inside the dedicated `Jooq{Noun}QueryAdapter`. Schema changes for them are applied via a manual, idempotent one-off script in `scripts/db/` (this repo has no Flyway/Liquibase) — run it against the target Postgres instance before `mvn generate-sources` so jOOQ codegen picks up the new column.

---

## 4. Change Rules (Safe Extension Points)

**Before implementing a feature:** if the flow you're about to touch (existing conventions, affected layers, risks, extension points) isn't already clear from this document or files you've read, run `/repo-discovery` first with details of the specific flow you intend to touch, to understand the current implementation before proposing changes.

**Adding a new external sync feed** (new source endpoint):
1. Port: add a fetch method to a new/existing client interface in `application/client/`.
2. Adapter: create a class in `infrastructure/client/` extending the existing abstract base class for API clients (pick the more specific one if the response shape already matches) — reuse the shared fetch/map/null-check helper, do not duplicate that logic.
3. Service: add a sync method in `application/service/`, transactional if it does delete+insert, using a **scoped delete** if sharing a table with another sync job (never delete-all on a shared table).
4. Scheduler: create a class in `infrastructure/scheduler/` extending the existing abstract base class for schedulers — reuse its shared safe-run helper, add only the startup-event and cron-trigger one-liners. Stagger the cron time from existing jobs if writing to the same table.
5. Cron property: add a new hyphenated `<entity>.sync.cron` property to config, matching the naming pattern of its siblings.
6. If the sync writes to the `set_cards` table, recompute the `representative` flag (`CardRepresentativeService.recompute()`) before refreshing the filter-options cache, then refresh the filter-options cache (`CardFilterOptionService.refresh()`) — both at the end of the sync method, inside the same transaction boundary, in that order (filter options must be derived from the already-recomputed flag). Follow the pattern of the existing sync jobs that already do this. Don't add a live/on-demand recompute path instead; the `representative` flag and the filter-options cache table are the single source of truth for, respectively, variant deduplication and the filters endpoint.

**Adding a new controller/endpoint:** follow the existing card controller as the reference — thin controller, full OpenAPI annotations (`@Tag`/`@Operation`/`@Parameter`/`@ApiResponses`) on every public method, delegate to a service, map via a dedicated mapper.

**Adding a new entity:** model in `application/model/`, repository interface in `application/repository/`, JPA or in-memory implementation in `infrastructure/persistence/`, DTO + mapper + controller in `web/`. Services depend only on the repository interface — persistence choice never touches service code.

---

## 5. Code Quality Rules

- **DI:** constructor injection only, via `@RequiredArgsConstructor` on `private final` fields. No `@Autowired` on fields.
- **Lombok:** `@Data` + `@Builder` + `@NoArgsConstructor` + `@AllArgsConstructor` on DTOs/domain models; `@RequiredArgsConstructor` on services/controllers; `@Slf4j` for logging (except shared abstract base classes where the logger must reflect the concrete subclass — use `LoggerFactory.getLogger(getClass())` there instead).
- **Naming:** `<Noun>Service`, `<Noun>Repository`, `<Noun>Controller`, `<Noun>Dto`, `<Noun>Mapper`. Service/scheduler method names must follow the same verb+entity pattern across sibling classes, not a mix of generic and specific names.
- **DTOs:** match frontend TypeScript interfaces exactly — camelCase fields, enums as uppercase `String`, `List<T>` never arrays, dates as ISO 8601 `String`, wrapper types (`Integer`, not `int`) for nullable fields.
- **Mappers:** explicit, no MapStruct. Enum→String via `.name()`. Handle dirty/unparseable source data by returning `null`/empty instead of throwing.
- **Config over hardcoding:** external base URLs and cron schedules live in `application.yml`, injected via constructor (`@Value` on a constructor parameter, never a field).
- **Comments:** Dont over-comment, only when it's not obvious from the code itself.

---

## 6. Testing Rules

- When adding tests:
  - **Services:** `@ExtendWith(MockitoExtension.class)`, mock repositories/clients, assert business logic in isolation.
  - **Controllers:** `@SpringBootTest` + `MockMvc`, assert HTTP status + JSON shape (enum casing, date format, field names) — this is the actual frontend contract, treat it as such.
  - Prefer testing behavior through the public port (service method, controller endpoint) over testing private helper methods directly.

---

## 7. Mandatory Requirements

- [ ] Enums serialized as **uppercase strings** in every DTO.
- [ ] Dates as ISO 8601 strings (`DateTimeFormatter.ISO_DATE_TIME`).
- [ ] Collections are always `List<T>`, never arrays, in DTOs and domain models.
- [ ] Every controller class has `@Tag`; every public method has `@Operation`, `@Parameter` on each param, `@ApiResponses` for non-trivial responses.
- [ ] Constructor injection everywhere (`@RequiredArgsConstructor`).
- [ ] Services throw `IllegalArgumentException` for "not found" (or a subtype, e.g. `NumberFormatException` from `Long.valueOf`) — a global exception handler maps it to 404. No custom exception classes.
- [ ] Repositories never throw — return `Optional`/empty `List`.
- [ ] Any new sync job sharing a table with an existing one uses a **scoped delete**, never `deleteAll()`.
- [ ] Cron property names and sync-service method names follow the existing verb+entity pattern (§5).
- [ ] Use var instead of explicit types for local variables

---

## 8. Forbidden Patterns

- ❌ Business logic in controllers or mappers — controllers delegate, mappers only convert.
- ❌ DTOs, Jackson, or other framework annotations inside `application/model/*`.
- ❌ Services returning DTOs — services return domain models; mapping happens in `web/mapper/*` or the controller.
- ❌ Lowercase enum serialization.
- ❌ Field injection (`@Autowired` on a field).
- ❌ Custom exception classes — use standard `IllegalArgumentException`/`MethodArgumentNotValidException`.
- ❌ MapStruct or reflection-based mapping — explicit mapping only.
- ❌ Premature abstraction / over-engineering: don't introduce a shared base class, config flag, or generic utility for a single use case. Only extract shared abstractions when ≥3 near-identical implementations already exist.
- ❌ `deleteAll()` on a table shared by multiple sync jobs.
- ❌ Hardcoding an external base URL inline in more than one class — externalize to `application.yml` instead.
- ❌ JOOQ `DSLContext`/generated record/table types leaking into `application/repository/*` ports or `application/service/*` — confine them to `infrastructure/persistence/*` adapters.

---

## 9. Output Expectations

Before considering a change complete, verify:

- Package placement respects §2's dependency direction (no layer violations).
- New/changed DTOs still match the frontend TypeScript contract exactly.
- OpenAPI annotations present on any new/changed controller method.
- `mvn clean compile` (and `mvn test`, once a test suite exists) passes.
- No stale references left behind after a rename (property names, method names) — grep for the old name.
- No exact code references in CLAUDE.md
- If the change alters architecture, naming conventions, or extension points described here, **update this file in the same change** — CLAUDE.md must stay in sync with the codebase it governs.

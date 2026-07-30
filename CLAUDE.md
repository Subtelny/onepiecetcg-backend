# One Piece TCG Backend - Rules for AI-Assisted Development

Spring Boot 4.1.0 / Java 21 REST backend for a One Piece TCG app. Consumed by a React frontend at `~/WebstormProjects/onepiecetcg`. This document is a **rulebook**, not a tutorial — read the referenced files for concrete patterns before writing code.

---

## 1. Project Context

- **Stack:** Java 21, Spring Boot 4.1.0 (WebMvc, Validation, Data JPA), JOOQ (complex/read-heavy queries only, see §3), SpringDoc OpenAPI 3.0, Lombok, PostgreSQL (Docker Compose).
- **Server:** port `3000`, base path `/api`. CORS origins are externalized to config (env-overridable), defaulting to the frontend's local dev and SSR ports — never hardcode them in `CorsConfig`.
- **Persistence status:**
  - Card sets and set/promo cards → PostgreSQL via Spring Data JPA.
  - Filter option values → PostgreSQL via Spring Data JPA, one row per distinct filterable value — a precomputed cache, not user-facing data.
  - Decks and shops → in-memory, not yet migrated to persistent storage.
- **External sync:** independent scheduled jobs pull from `https://www.optcgapi.com/api` (configurable via an externalized base-url property) and write into Postgres: card sets; set cards (a single job that fetches both regular set cards and promo cards from OPTCG and combines them). See §4 for the extension pattern they follow. Set-cards sync is gated behind card-sets diff detection: `SetCardSyncService` first calls `CardSetSyncService.syncCardSets()`, which returns `true` only if a card set present in the external `/allSets/` response is missing locally; if no new set is found, both the card-sets write and the expensive set-cards fetch + `CardRepresentativeService.recompute()` + `CardFilterOptionService.refresh()` are skipped entirely for that run.
- Swagger UI: `http://localhost:3000/swagger-ui.html`. OpenAPI spec: `http://localhost:3000/api-docs`.
- Reference key endpoints: `GET/POST/PUT/DELETE /api/decks`, `GET /api/decks/featured`, `GET/POST /api/shops`, `GET /api/cards`, `GET /api/cards/{id}`, `GET /api/deckbuilder/cards`, `GET /api/deckbuilder/cards/{id}`, `GET /api/home/*`, `/api/tournaments/*`.

---

## 2. Architecture Rules

Hexagonal architecture (Ports & Adapters) with DDD-flavored naming, organized into explicit **bounded contexts** as subpackages of the root `pl.janda.onepiecetcg`:

```
pl.janda.onepiecetcg/
├── application/                     # Root-only: app bootstrap (OnePieceTcgApplication), not a bounded context
├── web/
│   ├── controller/GlobalExceptionHandler.java   # Cross-cutting, shared by every bounded context
│   └── config/                      # Cross-cutting: CorsConfig, OpenApiConfig, InternalSecurityConfig, InternalApiKeyFilter
├── cards/                           # Bounded context: card catalog, search, sync
│   ├── application/
│   │   ├── model/                   # Domain POJOs (JPA-annotated only for persisted entities)
│   │   ├── repository/              # Repository interfaces (ports)
│   │   ├── client/                  # Outbound API client interfaces (ports)
│   │   └── service/                 # Business logic, incl. sync services
│   ├── infrastructure/
│   │   ├── persistence/             # Repository implementations (Jpa*/Jooq*)
│   │   ├── client/                  # HTTP client implementations (Optcg*)
│   │   └── scheduler/               # @Scheduled / @EventListener entrypoints
│   └── web/
│       ├── controller/              # CardController, InternalSyncController
│       ├── dto/
│       └── mapper/
└── deckbuilder/                     # Bounded context: deck-builder card browsing
    └── web/
        ├── controller/               # DeckBuilderCardController (/api/deckbuilder/cards)
        ├── dto/                      # Own DeckBuilderCard*Dto — not shared with cards.web.dto
        └── mapper/                   # DeckBuilderCardMapper — own explicit mapping, not shared with cards.web.mapper
```

`OnePieceTcgApplication` declares `@ComponentScan`, `@EnableJpaRepositories`, and `@EntityScan`, all explicitly `basePackages = "pl.janda.onepiecetcg"` — this is required so Spring picks up components/repositories/entities across every bounded-context subpackage; do not narrow these to a single bounded context.

**Dependency direction (enforced within each bounded context, no exceptions):**
```
web ──► application ◄── infrastructure
application ──X──► web            (forbidden)
application ──X──► infrastructure  (forbidden)
infrastructure ──X──► web          (forbidden)
```

**Cross-bounded-context dependency (the one deliberate, documented exception):** `deckbuilder.web` has no `application`/`infrastructure` layers of its own — it directly injects `cards.application.service.CardService` to reuse the existing search/filter/lookup logic (semantic search, variant resolution, filter-options cache) rather than forking it. This is intentional, not an oversight: `deckbuilder` only needs card-browsing behavior identical to `cards`, with no deck-specific business rules yet, so introducing an anti-corruption layer or a shared/common package for a single consumer would be premature abstraction. If `deckbuilder` ever needs behavior that diverges from `cards`, add it in `deckbuilder`'s own application layer at that point — don't fork `CardService` preemptively.

**DTO/mapper duplication (accepted, deliberate exception to the dedup rule in §8):** `cards.web.dto`/`cards.web.mapper` and `deckbuilder.web.dto`/`deckbuilder.web.mapper` intentionally define separate, near-identical DTOs and mappers (e.g. `CardDto`/`DeckBuilderCardDto`) even though their shapes look the same today. Bounded contexts don't share web-facing contracts — a change to one context's response shape (e.g. adding deck-specific fields to `DeckBuilderCardDto` later) must not silently ripple into the other's frontend contract. Do not "clean this up" by extracting a shared DTO/mapper.

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
- Postgres-native generated columns that jOOQ codegen doesn't model as a plain typed field (currently `set_cards.card_semantic_search_vector`, a `GENERATED ALWAYS ... STORED` tsvector backing SEMANTIC full-text search) are never added to the JPA entity — they're written/read only via raw JOOQ SQL escape hatches (`field()`/`condition()` templates) inside the dedicated `Jooq{Noun}QueryAdapter`.
- **Schema ownership is split, and both halves are automatic.** This repo has no Flyway/Liquibase: JPA-mapped tables/columns come from Hibernate `ddl-auto: update` (in every profile, `prod` included), and the generated columns Hibernate cannot express come from an idempotent DDL script under `src/main/resources/db/`, applied on every boot by an `ApplicationRunner` in `cards/infrastructure/persistence`. There is no manual per-environment schema step; do not reintroduce one. An `ApplicationRunner` is required rather than an `ApplicationReadyEvent` listener because the sync schedulers key off `ApplicationReadyEvent` and would otherwise query the column before it exists.
  - The script is the single source of truth for those columns — use `IF NOT EXISTS` throughout so re-running it on a migrated database is a no-op, and never duplicate its DDL into test setup (integration tests get the column from application startup, which is the point).
  - A missing generated column surfaces as `BadSqlGrammarException`, not as an obvious "no such column": Postgres reports SQLState 42703 and Spring maps the whole class-42 range to bad-SQL-grammar. Treat that exception on a search endpoint as a schema-drift suspect first.
  - Postgres has no `ALTER COLUMN` for a `GENERATED` expression, so **changing** an expression means: edit the script, run the matching drop script from `scripts/db/` against the target database, restart. `scripts/db/` therefore holds only teardown/one-off scripts, never the canonical definition. Do the same against local Postgres before `mvn generate-sources` so jOOQ codegen sees the column.

---

## 4. Change Rules (Safe Extension Points)

**Before implementing a feature:** if the flow you're about to touch (existing conventions, affected layers, risks, extension points) isn't already clear from this document or files you've read, run `/repo-discovery` first with details of the specific flow you intend to touch, to understand the current implementation before proposing changes.

**Adding a new external sync feed** (new source endpoint):
1. Port: add a fetch method to a new/existing client interface in `application/client/`.
2. Adapter: create a class in `infrastructure/client/` extending the existing abstract base class for API clients (pick the more specific one if the response shape already matches) — reuse the shared fetch/map/null-check helper, do not duplicate that logic.
3. Service: add a sync method in `application/service/`, using a **scoped delete** if sharing a table with another sync job (never delete-all on a shared table). Keep the remote fetch **outside** the transaction: an HTTP call inside `@Transactional` holds a database connection open for the whole round trip. Where a sync does delete+insert, the orchestrating service stays non-transactional (fetch, decide whether to run, map) and delegates the write to a separate `@Transactional` replace method, so the delete and the insert commit or roll back together. A `@Transactional` method must never swallow its own exceptions — catching inside the transaction lets a half-finished replace commit (e.g. the delete without the insert); catch in the non-transactional caller instead.
4. Scheduler: create a class in `infrastructure/scheduler/` extending the existing abstract base class for schedulers — reuse its shared safe-run helper, add only the startup-event and cron-trigger one-liners. Stagger the cron time from existing jobs if writing to the same table.
5. Cron property: add a new hyphenated `<entity>.sync.cron` property to config, matching the naming pattern of its siblings.
6. If the sync writes to the `set_cards` table, recompute the `representative` flag (`CardRepresentativeService.recompute()`) before refreshing the filter-options cache, then refresh the filter-options cache (`CardFilterOptionService.refresh()`) — both at the end of the transactional replace method described in step 3, in that order (filter options must be derived from the already-recomputed flag). Follow the pattern of the existing sync jobs that already do this. Don't add a live/on-demand recompute path instead; the `representative` flag and the filter-options cache table are the single source of truth for, respectively, variant deduplication and the filters endpoint.

**Adding a new controller/endpoint:** follow `cards.web.controller.CardController` as the reference — thin controller, full OpenAPI annotations (`@Tag`/`@Operation`/`@Parameter`/`@ApiResponses`) on every public method, delegate to a service, map via a dedicated mapper. Place it in the bounded context it belongs to (`cards.web.*`, `deckbuilder.web.*`, or a new bounded-context subpackage) rather than the root `web/` package, which is reserved for cross-cutting concerns (`GlobalExceptionHandler`, `web/config/*`).

**Adding a new entity:** model in `<context>/application/model/`, repository interface in `<context>/application/repository/`, JPA or in-memory implementation in `<context>/infrastructure/persistence/`, DTO + mapper + controller in `<context>/web/`. Services depend only on the repository interface — persistence choice never touches service code.

**Adding a new bounded context:** mirror the `cards`/`deckbuilder` layout — a new top-level subpackage of `pl.janda.onepiecetcg` with its own `application`/`infrastructure`/`web` layers (or just `web` if it's a pure consumer of another context's service, like `deckbuilder`). Don't add a shared/common package for cross-context reuse unless ≥3 bounded contexts would need it (see §8) — a single consumer should depend directly on the owning context's `application/service/*`, as `deckbuilder` does on `cards`.

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
  - **Persistence adapters:** `@SpringBootTest` + Testcontainers Postgres wired via `@DynamicPropertySource`, asserting real SQL behavior (the point is the database, so don't mock it away). Append `prepareThreshold=0` to the container JDBC URL — startup DDL adds a column mid-session, and a cached server-side plan otherwise fails with "cached plan must not change result type".
  - Any `@SpringBootTest` must `@MockitoBean` every sync service, in every bounded context. `ApplicationReadyEvent` fires in tests, so the schedulers would otherwise hit the real external API and replace the table contents underneath the fixtures.
  - Never re-create schema that application startup already creates (see §3) — a test that sets up its own copy of the DDL stops proving the shipped schema is correct and silently drifts from it.
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
- [ ] Remote fetches happen outside `@Transactional`; delete+insert replaces sit inside one, and that transactional method does not catch its own exceptions.
- [ ] New non-JPA-expressible DDL goes in the startup script under `src/main/resources/db/`, guarded by `IF NOT EXISTS` — not in a manual-only script and not duplicated in tests.
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
- ❌ Premature abstraction / over-engineering: don't introduce a shared base class, config flag, or generic utility for a single use case. Only extract shared abstractions when ≥3 near-identical implementations already exist. (Exception: `deckbuilder.web` and `cards.web` intentionally keep separate, near-identical DTOs/mappers — see §2 — because they're different bounded-context contracts, not incidental duplication.)
- ❌ `deleteAll()` on a table shared by multiple sync jobs.
- ❌ Hardcoding an external base URL inline in more than one class — externalize to `application.yml` instead.
- ❌ JOOQ `DSLContext`/generated record/table types leaking into `application/repository/*` ports or `application/service/*` — confine them to `infrastructure/persistence/*` adapters.
- ❌ A schema step that has to be run by hand per environment. Whatever Hibernate can't express belongs in the startup DDL script (§3); anything else is a step someone will skip, and the skipped environment fails at request time rather than at boot.
- ❌ `try`/`catch` around the body of a `@Transactional` method, or an outbound HTTP call inside one.
- ❌ Adding a JOOQ codegen include for a table that no longer exists — codegen then fails loudly on a clean build, and the stale include hides which tables the adapters actually need.

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

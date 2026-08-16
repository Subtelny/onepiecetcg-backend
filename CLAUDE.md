# One Piece TCG Backend - Rules for AI-Assisted Development

Spring Boot 4.1.0 / Java 21 REST backend for a One Piece TCG app. Consumed by a React frontend at `~/WebstormProjects/onepiecetcg`. This document is a **rulebook**, not a tutorial — read the referenced files for concrete patterns before writing code.

---

## 1. Project Context

- **Stack:** Java 21, Spring Boot 4.1.0 (WebMvc, Validation, Data JPA), JOOQ (complex/read-heavy queries only, see §3), SpringDoc OpenAPI 3.0, Lombok, PostgreSQL (Docker Compose).
- **Server:** port `3000`, base path `/api`. CORS origins are externalized to config (env-overridable), defaulting to the frontend's local dev and SSR ports — never hardcode them in `CorsConfig`.
- **Persistence status:**
  - Card sets and set/promo cards → PostgreSQL via Spring Data JPA.
  - Filter option values → PostgreSQL via Spring Data JPA, one row per distinct filterable value — a precomputed cache, not user-facing data.
  - Immutable shared deck snapshots → PostgreSQL via Spring Data JPA. They store stable card numbers rather than
    `set_cards` identity IDs because catalog sync replaces that table.
  - Pricing history, Cardmarket expansion mappings, and Cardmarket single mappings → PostgreSQL via Spring Data JPA in
    the `pricing` bounded context. Price mappings reference catalog items by deterministic text keys, never by
    replaceable `set_cards` identity IDs.
  - Browsable community decks and shops → not yet migrated to persistent storage.
- **Card catalog sync:** independent scheduled jobs read the scraper-populated `onepiece_card_sets` and `onepiece_cards`
  tables and write the application-facing `card_sets` and `set_cards` tables. `SetCardSyncService` maps every printed
  variant from the source table, derives its text `variant_index` directly from `onepiece_cards.id` (`0` for the
  unsuffixed default print, `pN` for a parallel, `rN` for a reprint), replaces `set_cards` transactionally, and
  refreshes filter options on every run. A representative card is always the row with `variant_index = '0'`; there is no
  separate representative flag or post-insert index recomputation. Each print also receives a stable, namespaced
  `price_reference` in the form `single:<onepiece_cards.id>` so pricing associations survive table replacement.
  - A second group of sync jobs scrapes the official rules site (errata notices as HTML, FAQ as per-set PDFs). Their adapters use Jsoup/PDFBox and follow the same service/scheduler pattern as everything else (§4).
  - **Every sync that does delete+insert uses the orchestrator + replacement split described in §4 step 3** — there are no remaining syncs where the fetch happens inside the transaction. Don't reintroduce one.
- **Pricing sync:** the `pricing` bounded context owns Cardmarket clients, scheduling, price history, enrichment, and
  matching. It maps each Cardmarket expansion to a catalog release **offline, by card-code containment** — the share of
  the expansion's card codes that the release prints, accepted above a threshold and rejected on a tie so an ambiguous
  expansion stays unmapped rather than mismapping every card in it. Card codes are the only identifier both sides spell
  identically: catalog release names carry a set code and product-type prefix that Cardmarket's never have, so name
  matching resolved almost nothing, and the product-page scrape it depended on is 403-blocked. Don't reintroduce an
  HTTP call here — the matcher is pure and runs against data already fetched. It also creates the expansion rows it
  scores, and rescores every expansion on each run, since the catalog grows and a previously unplaceable expansion
  becomes placeable for free. Singles then match by `card code + release + local variant`, preferring `V1`/`V2` parsed
  from the product name, with product date/ID ordering persisted as an explicitly lower-confidence heuristic when no
  version metadata exists. The context stores durable Cardmarket-product-to-`price_reference` mappings separately from append-only
  price snapshots. Its source-neutral query port exposes two reads over opaque price references: a batch lookup of the
  newest mapped quote per reference, and a single-reference history. Card search and detail REST adapters use the batch
  lookup to expose prices without one query per card. The history is deduplicated **on read** — snapshots stay stored
  unfiltered so the sync's "already imported this price guide" guard keeps working, and a window function drops every
  snapshot whose trend and low both match the previous one. A gap in the resulting series therefore means "price held",
  so consumers carry the last point forward and never interpolate. Only single-card responses embed that history; the
  variants endpoint leaves it empty rather than paying one window query per printed variant.
- **Deployment memory budget:** prod runs in a 1 GB container, so heap/metaspace are capped by JVM flags in `Procfile` and the Tomcat/Hikari/`@Async` pools are capped in `application-prod.yml`. Both are documented in `DEPLOYMENT.md` — when adding a feature that holds a whole table in memory or spawns threads, that budget is the constraint to design against.
- Swagger UI: `http://localhost:3000/swagger-ui.html`. OpenAPI spec: `http://localhost:3000/api-docs`. Both are **disabled in the `prod` profile** — the spec would otherwise publish every route, `/api/internal/*` included, to anyone. Keep the OpenAPI annotations mandatory anyway (§7): they're the contract documentation for local/dev consumers, and the frontend is verified against the local Swagger UI.
- Reference key endpoints: `GET /` and `GET /health` (liveness), `GET /api/cards`, `GET /api/cards/{id}`,
  `GET /api/deckbuilder/cards`, `GET /api/deckbuilder/cards/{id}`, `POST /api/deckbuilder/shared-decks`, and
  `GET /api/deckbuilder/shared-decks/{code}`.

---

## 2. Architecture Rules

Hexagonal architecture (Ports & Adapters) with DDD-flavored naming, organized into explicit **bounded contexts** as subpackages of the root `pl.janda.onepiecetcg`:

```
pl.janda.onepiecetcg/
├── OnePieceTcgApplication.java      # App bootstrap, not a bounded context
├── infrastructure/                 # Cross-cutting infrastructure
│   ├── web/
│   │   ├── controller/              # GlobalExceptionHandler
│   │   └── config/                  # CORS, OpenAPI, internal endpoint security
│   └── status/
│       ├── controller/              # Railway liveness endpoints
│       └── dto/
├── cards/                           # Bounded context: card catalog, search, sync
│   ├── application/
│   │   ├── model/                   # Domain POJOs (JPA-annotated only for persisted entities)
│   │   ├── port/in/                 # Use-case interfaces consumed by controllers/schedulers
│   │   ├── repository/              # Outbound persistence interfaces (ports)
│   │   ├── client/                  # Outbound API client interfaces (ports)
│   │   └── service/                 # Use-case implementations and transaction orchestration
│   ├── infrastructure/
│   │   ├── persistence/             # Repository implementations (Jpa*/Jooq*)
│   │   ├── client/                  # HTTP/scraper client implementations
│   │   └── scheduler/               # @Scheduled / @EventListener entrypoints
│   └── rest/
│       ├── controller/              # CardController, InternalSyncController
│       ├── dto/
│       └── mapper/
├── pricing/                         # Bounded context: price collection and source/catalog mapping
│   ├── application/
│   │   ├── model/                   # Price snapshots, expansion mappings, single mappings
│   │   ├── port/in/                 # Pricing sync and source-neutral price query use cases
│   │   ├── repository/              # Pricing persistence ports
│   │   ├── client/                  # Cardmarket and catalog client ports
│   │   └── service/                 # Enrichment, matching, import orchestration
│   ├── infrastructure/
│   │   ├── persistence/             # Spring Data adapters + startup DDL initializer (§3)
│   │   ├── client/                  # Cardmarket HTTP and card-catalog adapters
│   │   └── scheduler/               # Pricing-only scheduled entrypoints
│   └── rest/                        # Pricing-owned internal sync endpoint
└── deckbuilder/                     # Bounded context: deck building and immutable shared snapshots
    ├── application/
    │   ├── model/                   # Shared-deck commands, persisted snapshot, hydrated details
    │   ├── port/in/                 # SharedDeckUseCase
    │   ├── repository/              # SharedDeckRepository outbound port
    │   └── service/                 # Snapshot validation, short-code generation, catalog hydration
    ├── infrastructure/persistence/  # Spring Data adapter for shared_decks/shared_deck_cards
    └── rest/
        ├── controller/              # Card browsing and shared-deck endpoints
        ├── dto/                     # Own DeckBuilder*/SharedDeck* contracts
        └── mapper/                  # Explicit context-owned mappings
```

`OnePieceTcgApplication` declares `@ComponentScan`, `@EnableJpaRepositories`, and `@EntityScan`, all explicitly `basePackages = "pl.janda.onepiecetcg"` — this is required so Spring picks up components/repositories/entities across every bounded-context subpackage; do not narrow these to a single bounded context.

**Dependency direction (enforced within each bounded context, no exceptions):**
```
rest ──► application ◄── infrastructure
application ──X──► rest           (forbidden)
application ──X──► infrastructure  (forbidden)
infrastructure ──X──► rest         (forbidden)
```

**Cross-bounded-context dependency (the deliberate, documented exception):** `deckbuilder` injects
`cards.application.port.in.CardCatalogUseCase` rather than forking catalog behavior. Its card-browsing controller uses
the shared search/filter/lookup behavior directly, while `SharedDeckService` uses the bulk representative-card lookup to
validate and hydrate stable card-number references. Shared-deck persistence deliberately has no foreign key to
`set_cards`: catalog sync replaces that table and its generated identity IDs are not durable references.

`pricing.infrastructure.client` similarly adapts the narrow card-owned priceable-catalog inbound port into pricing's own
application model. The `pricing.application` layer therefore has no dependency on `cards`. The cards and deckbuilder
REST adapters compose their catalog responses with the source-neutral pricing query port; their application layers
remain independent of pricing. The durable association is the opaque namespaced `price_reference`; do not add a database
foreign key or share JPA entities across these contexts.

**DTO/mapper duplication (accepted, deliberate exception to the dedup rule in §8):** `cards.rest.dto`/
`cards.rest.mapper` and `deckbuilder.rest.dto`/`deckbuilder.rest.mapper` intentionally define separate, near-identical
DTOs and mappers (e.g. `CardDto`/`DeckBuilderCardDto`) even though their shapes look the same today. Bounded contexts
don't share REST-facing contracts — a change to one context's response shape (e.g. adding deck-specific fields to
`DeckBuilderCardDto` later) must not silently ripple into the other's frontend contract. Do not "clean this up" by
extracting a shared DTO/mapper.

---

## 3. Hexagonal Rules (Ports & Adapters)

- `application/port/in/*` contains **inbound use-case ports**. Controllers, schedulers, and cross-context consumers
  depend on these interfaces, never on concrete `@Service` classes.
- `application/repository/*` and `application/client/*` are **outbound ports** — interfaces only, no implementation, no
  framework types beyond return values (`Optional<T>`, `List<T>`).
- `infrastructure/persistence/*` and `infrastructure/client/*` are **adapters** — implement the ports, contain all framework/HTTP/SQL-specific code.
- Spring Data interfaces are package-private infrastructure details and never extend an application repository port
  directly. A `Jpa{Noun}Repository` adapter implements the application port and delegates simple operations to a
  `{Noun}JpaRepository` Spring Data interface; this keeps framework-generated interfaces separate from application
  contracts.
- One port method per external capability — do not bundle unrelated operations into one method.
- Schedulers (`infrastructure/scheduler/*`) and controllers (`rest/controller/*`) are **entrypoints**: they call
  `application/port/in/*`, never concrete services or outbound adapters directly.
- Large request parameter lists do not cross layers. Web adapters map their DTOs to an application input object (for
  card search: `CardSearchQuery`); the service resolves defaults/semantic shorthand into persistence criteria
  (`CardSearchCriteria`) before calling an outbound port.
- Derived-query methods with no custom filtering logic are declared directly on the JPA repository interface (Spring Data generates them) — no need for a manual implementation.
- Complex/read-heavy queries (multi-field dynamic search/filtering, cross-row aggregation, bulk recompute) are implemented with **JOOQ** (`DSLContext`) in a dedicated class in `infrastructure/persistence/` (e.g. `Jooq{Noun}QueryAdapter`), called from the corresponding `Jpa{Noun}Repository` method — pushes filtering, sorting, pagination (`LIMIT`/`OFFSET`), counting, and grouping to the database instead of loading full tables into memory and processing them with Java Streams. This superseded the earlier `findAll()` + Streams default-method convention for these cases; simple derived queries with no such logic still don't need it.
- JOOQ generated sources (jOOQ codegen, target `generated-sources`), `DSLContext`, and JOOQ record/table types are adapter-only — they must never appear in `application/repository/*` port signatures or in `application/service/*`; ports keep returning plain domain types (`Optional<T>`, `List<T>`).
- If a JPA-backed adapter method (e.g. `saveAll`) is followed, within the same transaction, by a JOOQ query reading/writing the same table (raw JDBC via `DSLContext`, bypassing the Hibernate session), that JPA adapter method must flush the persistence context first (e.g. `saveAllAndFlush` instead of `saveAll`) — otherwise the JOOQ query can see stale or incomplete data, since Hibernate cannot auto-flush ahead of non-Hibernate-session queries.
- Batched bulk writes must `EntityManager.clear()` **after** each batch's flush, never instead of it. Without the clear, every saved entity stays managed for the rest of the transaction — each costing an `EntityEntry` plus a loaded-state snapshot on top of the entity itself — so peak heap scales with the whole write instead of one batch, which the prod memory budget (§1) doesn't allow. Clearing is safe alongside the flush rule above precisely because it happens after the flush: the rows are already visible to the rest of the transaction, and raw-JDBC JOOQ reads don't need managed entities. Keep the batch size equal to `hibernate.jdbc.batch_size`, otherwise a "batch" never maps to one JDBC round trip.
- Bulk write adapter methods return `void`. Returning the saved entities builds a second full copy of the data for a caller that only ever wanted the row count — and it already has that from the argument it passed in.
- Postgres-native generated columns that jOOQ codegen doesn't model as a plain typed field (currently `set_cards.card_semantic_search_vector`, a `GENERATED ALWAYS ... STORED` tsvector backing SEMANTIC full-text search) are never added to the JPA entity — they're written/read only via raw JOOQ SQL escape hatches (`field()`/`condition()` templates) inside the dedicated `Jooq{Noun}QueryAdapter`.
- **Schema ownership is split, and both halves are automatic.** This repo has no Flyway/Liquibase: JPA-mapped tables/columns come from Hibernate `ddl-auto: update` (in every profile, `prod` included), and the generated columns Hibernate cannot express come from an idempotent DDL script under `src/main/resources/db/`, applied on every boot by an `ApplicationRunner` in `cards/infrastructure/persistence`. There is no manual per-environment schema step; do not reintroduce one. An `ApplicationRunner` is required rather than an `ApplicationReadyEvent` listener because the sync schedulers key off `ApplicationReadyEvent` and would otherwise query the column before it exists.
  - The script is the single source of truth for those columns — use `IF NOT EXISTS` throughout so re-running it on a migrated database is a no-op, and never duplicate its DDL into test setup (integration tests get the column from application startup, which is the point).
  - A missing generated column surfaces as `BadSqlGrammarException`, not as an obvious "no such column": Postgres reports SQLState 42703 and Spring maps the whole class-42 range to bad-SQL-grammar. Treat that exception on a search endpoint as a schema-drift suspect first.
  - Postgres has no `ALTER COLUMN` for a `GENERATED` expression, so **changing** an expression means: edit the script, run the matching drop script from `scripts/db/` against the target database, restart. `scripts/db/` therefore holds only teardown/one-off scripts, never the canonical definition. Do the same against local Postgres before `mvn generate-sources` so jOOQ codegen sees the column.
  - **Renaming or removing an `@Enumerated(STRING)` constant is a schema change, even though nothing in the entity looks
    like one.** Hibernate writes a `CHECK (col IN (...))` constraint when it first creates the table and never revisits
    it under `ddl-auto: update`, so a database created before the rename keeps rejecting the new value: the code passes
    tests and fresh installs, then fails at write time on every migrated environment. Realign the constraint in that
    context's startup DDL script with a `DROP CONSTRAINT IF EXISTS` + `ADD CONSTRAINT` pair guarded by
    `pg_advisory_xact_lock` (the pair is what makes it idempotent — Postgres has no `ADD CONSTRAINT IF NOT EXISTS`).
    Each bounded context owns its own script and `*SchemaInitializer`; follow the existing ones rather than adding a
    shared initializer.

---

## 4. Change Rules (Safe Extension Points)

**Before implementing a feature:** if the flow you're about to touch (existing conventions, affected layers, risks, extension points) isn't already clear from this document or files you've read, run `/repo-discovery` first with details of the specific flow you intend to touch, to understand the current implementation before proposing changes.

**Adding a new external sync feed** (new source endpoint):
1. Port: add a fetch method to a new/existing client interface in `application/client/`.
2. Adapter: create a class in `infrastructure/client/` extending the existing abstract base class for API clients (pick the more specific one if the response shape already matches) — reuse the shared fetch/map/null-check helper, do not duplicate that logic.
3. Service: add a sync method in `application/service/`, using a **scoped delete** if sharing a table with another sync job (never delete-all on a shared table). Keep the remote fetch **outside** the transaction: an HTTP call inside `@Transactional` holds a database connection open for the whole round trip. Where a sync does delete+insert, split it across **two classes**, which is the convention every existing sync now follows:
    - `<Noun>SyncService` — the orchestrator implementing a matching `application/port/in/<Noun>SyncUseCase`, **not**
      `@Transactional`. Owns the fetch, the decision whether to run at all, the mapping, and the `lastSyncedAt` stamp.
      Plain reads used only to decide whether to skip (e.g. an up-to-date check) belong here too; they don't need a
      transaction.
   - `<Noun>ReplacementService` — `@Transactional`, one method (`replaceAll`/`replaceSet`) doing just the delete + insert, so the two commit or roll back together.

   Scope that transaction as tightly as the delete allows: where the delete is already set-scoped, replace **one set per transaction** inside the orchestrator's loop rather than wrapping the whole loop — it keeps atomicity at the right granularity and bounds the persistence context to one set's rows instead of the entire run's. A `@Transactional` method must never swallow its own exceptions — catching inside the transaction lets a half-finished replace commit (e.g. the delete without the insert); let it propagate to the scheduler's safe-run helper (step 4) instead.

   This split is a transaction boundary, not a reuse abstraction — so it applies even with a single caller, and the §8 "no premature abstraction" rule does not override it.
4. Scheduler: create a class in `infrastructure/scheduler/` extending the existing abstract base class for schedulers — reuse its shared safe-run helper, add only the startup-event and cron-trigger one-liners. Stagger the cron time from existing jobs if writing to the same table.
5. Cron property: add a new hyphenated `<entity>.sync.cron` property to config, matching the naming pattern of its siblings.
6. If the sync writes to the `set_cards` table, derive `variant_index` while mapping each source row, before entering
   the replacement transaction, then refresh the filter-options cache (`CardFilterOptionService.refresh()`) after the
   insert at the end of the transactional replacement method described in step 3. Follow the pattern of the existing
   sync job. Don't add a post-insert recompute path: the source-derived `variant_index = '0'` rows and the
   filter-options cache table are the single sources of truth for, respectively, variant deduplication and the filters
   endpoint.

**Adding a new controller/endpoint:** follow `cards.rest.controller.CardController` as the reference — thin controller,
full OpenAPI annotations (`@Tag`/`@Operation`/`@Parameter`/`@ApiResponses`) on every public method, delegate to an
inbound use-case port, map via a dedicated mapper. Place it in the bounded context it belongs to (`cards.rest.*`,
`deckbuilder.rest.*`, or a new bounded-context subpackage) rather than the root `infrastructure.web` package, which is
reserved for cross-cutting concerns (`GlobalExceptionHandler`, `infrastructure/web/config/*`).

**Adding a new entity:** model in `<context>/application/model/`, repository interface in
`<context>/application/repository/`, JPA or in-memory implementation in `<context>/infrastructure/persistence/`, DTO +
mapper + controller in `<context>/rest/`. Services depend only on the repository interface — persistence choice never
touches service code.

**Adding a new bounded context:** mirror the `cards`/`deckbuilder` layout — a new top-level subpackage of
`pl.janda.onepiecetcg` with its own `application`/`infrastructure`/`rest` layers (or just `rest` if it's a pure consumer
of another context's use-case port, like `deckbuilder`). Don't add a shared/common package for cross-context reuse
unless ≥3 bounded contexts would need it (see §8) — a single consumer should depend directly on the owning context's
`application/port/in/*`.

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
- Any `@SpringBootTest` must `@MockitoBean` every sync use-case port, in every bounded context.
    `ApplicationReadyEvent` fires in tests, so the schedulers would otherwise hit the real external API and replace the
    table contents underneath the fixtures.
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
- [ ] Remote fetches happen outside `@Transactional`; delete+insert replaces sit inside one via a `<Noun>ReplacementService` (§4 step 3), and that transactional method does not catch its own exceptions.
- [ ] Batched bulk writes clear the persistence context after each flush, use a batch size matching `hibernate.jdbc.batch_size`, and return `void` (§3).
- [ ] No unbounded full-table read (`findAll()` on a table that grows with the catalog) is reachable from a controller — push filtering/pagination into the query (§3). `findAll()` is acceptable only on the small bounded tables (card sets, errata, filter options).
- [ ] New non-JPA-expressible DDL goes in the startup script under `src/main/resources/db/`, guarded by `IF NOT EXISTS` — not in a manual-only script and not duplicated in tests.
- [ ] Cron property names and sync-service method names follow the existing verb+entity pattern (§5).
- [ ] Use var instead of explicit types for local variables

---

## 8. Forbidden Patterns

- ❌ Business logic in controllers or mappers — controllers delegate, mappers only convert.
- ❌ DTOs, Jackson, or other framework annotations inside `application/model/*`.
- ❌ Services returning DTOs — services return domain models; mapping happens in `rest/mapper/*` or the controller.
- ❌ Lowercase enum serialization.
- ❌ Field injection (`@Autowired` on a field).
- ❌ Custom exception classes — use standard `IllegalArgumentException`/`MethodArgumentNotValidException`.
- ❌ MapStruct or reflection-based mapping — explicit mapping only.
- ❌ Premature abstraction / over-engineering: don't introduce a shared base class, config flag, or generic utility for a
  single use case. Only extract shared abstractions when ≥3 near-identical implementations already exist. (Exception:
  `deckbuilder.rest` and `cards.rest` intentionally keep separate, near-identical DTOs/mappers — see §2 — because
  they're different bounded-context contracts, not incidental duplication.)
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

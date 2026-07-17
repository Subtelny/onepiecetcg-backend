# One Piece TCG Backend - Rules for AI-Assisted Development

Spring Boot 3.4.1 / Java 21 REST backend for a One Piece TCG app. Consumed by a React frontend at `~/WebstormProjects/onepiecetcg`. This document is a **rulebook**, not a tutorial — read the referenced files for concrete patterns before writing code.

---

## 1. Project Context

- **Stack:** Java 21, Spring Boot 3.4.1 (Web, Validation, Data JPA), SpringDoc OpenAPI 3.0, Lombok, PostgreSQL (Docker Compose).
- **Server:** port `3000`, base path `/api`, CORS enabled for `http://localhost:5173`.
- **Persistence status:**
  - `CardSet`, `SetCard` → PostgreSQL via Spring Data JPA (`JpaCardSetRepository`, `JpaSetCardRepository`).
  - `Deck`, `Shop` → in-memory `ConcurrentHashMap` (`InMemoryDeckRepository`, `InMemoryShopRepository`), not yet migrated.
- **`/api/cards` is served directly from `SetCard`** (synced from optcgapi.com) — there is no separate mocked `Card` entity. `SetCard` holds **both** regular set cards and promo cards in one `set_cards` table, distinguished by `SetCard.promo` (boolean). This is the frontend's card-search data source (see `onepiecetcg` `CLAUDE.md`) — `GET /api/cards` accepts `name` (matches card name or card number, contains/case-insensitive) plus multi-value filters `types`, `color`, `rarity`, `setIds`, `attributes` (each OR-matched against the card, all params ANDed together), and exact-match `cost`/`power`/`counterAmount`/`subTypes`. `GET /api/cards/filters` returns the full set of valid values for `types`/`color`/`rarity`/`attributes`/sets — enum-backed filter values (`types`, `color`, `rarity`) must be sent exactly as returned there (Java `valueOf()` is case-sensitive). `CardDto` also exposes `marketPrice`/`inventoryPrice` (nullable `Double`, from the synced data).
- **External sync:** three independent scheduled jobs pull from `https://www.optcgapi.com/api` (configurable via `optcgapi.base-url`) and write into Postgres: card sets, set cards, promo cards. See §4 for the extension pattern they follow.
- Swagger UI: `http://localhost:3000/swagger-ui.html`. OpenAPI spec: `http://localhost:3000/api-docs`.
- Reference key endpoints: `GET/POST/PUT/DELETE /api/decks`, `GET /api/decks/featured`, `GET/POST /api/shops`, `GET /api/cards`, `GET /api/cards/{id}`, `GET /api/home/*`, `/api/tournaments/*`.

---

## 2. Architecture Rules

Hexagonal architecture (Ports & Adapters) with DDD-flavored naming. Package layout:

```
pl.janda.onepiecetcg/
├── application/        # Core — framework-independent (except @Service/@Transactional)
│   ├── model/           # Domain POJOs (JPA-annotated only for CardSet/SetCard)
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
- One port method per external capability (e.g. `fetchAllSetCards()`, `deleteByPromo(boolean)`) — do not bundle unrelated operations into one method.
- Schedulers (`infrastructure/scheduler/*`) and controllers (`web/controller/*`) are **entrypoints**: they call `application/service/*`, never call adapters directly.
- Derived-query methods with no custom filtering logic (e.g. `findById`, `deleteByPromo`) are declared directly on the `Jpa*Repository` interface (Spring Data generates them) — no need for a manual implementation.
- Methods that need Java-side filtering (no matching derived-query shape) are implemented as **default methods** on the `Jpa*Repository` interface using `findAll()` + Streams (see `SetCardRepository.search(...)`) — avoids the Specifications/Criteria API.

---

## 4. Change Rules (Safe Extension Points)

**Before implementing a feature with unclear code context:** if the flow you're about to touch (existing conventions, affected layers, risks, extension points) isn't already clear from this document or files you've read, run `/repo-discovery` first with details of the specific flow you intend to touch, to understand the current implementation before proposing changes.

**Adding a new optcgapi.com sync feed** (new endpoint, e.g. `/allBans/`):
1. Port: add `fetchAllX()` to a new/existing `application/client/*ApiClient` interface.
2. Adapter: create a class in `infrastructure/client/` extending `AbstractOptcgApiClient` (or `AbstractSetCardApiClient` if the response shape matches `OptcgSetCardResponse`) — reuse `fetchAndMap(uri, ResponseType[].class, mapper)`, do not duplicate the fetch/null-check/stream logic.
3. Service: add a `sync<Entity>()` method in `application/service/`, `@Transactional` if it does delete+insert, using a **scoped delete** if sharing a table with another sync job (never `deleteAll()` on a shared table).
4. Scheduler: create a class in `infrastructure/scheduler/` extending `AbstractSyncScheduler` — reuse `runSyncSafely(Runnable, String)`, add only the `@EventListener`/`@Scheduled` one-liners. Stagger the cron time from existing jobs if writing to the same table.
5. Cron property: add `<entity>.sync.cron` to `application.yml`, hyphenated to match siblings (`card-sets.sync.cron`, `set-cards.sync.cron`, `promo-cards.sync.cron`).

**Adding a new controller/endpoint:** follow `CardController` as the reference — thin controller, `@Tag`/`@Operation`/`@Parameter`/`@ApiResponses` on every public method, delegate to a service, map via a `web/mapper/*Mapper`.

**Adding a new entity:** model in `application/model/`, repository interface in `application/repository/`, JPA or in-memory implementation in `infrastructure/persistence/`, DTO + mapper + controller in `web/`. Services depend only on the repository interface — persistence choice never touches service code.

---

## 5. Code Quality Rules

- **DI:** constructor injection only, via `@RequiredArgsConstructor` on `private final` fields. No `@Autowired` on fields.
- **Lombok:** `@Data` + `@Builder` + `@NoArgsConstructor` + `@AllArgsConstructor` on DTOs/domain models; `@RequiredArgsConstructor` on services/controllers; `@Slf4j` for logging (except shared abstract base classes where the logger must reflect the concrete subclass — use `LoggerFactory.getLogger(getClass())` there instead, see `AbstractSyncScheduler`).
- **Naming:** `<Noun>Service`, `<Noun>Repository`, `<Noun>Controller`, `<Noun>Dto`, `<Noun>Mapper`. Service/scheduler method names must follow the same verb+entity pattern across sibling classes (e.g. `syncCardSets`/`syncSetCards`/`syncPromoCards`, not a mix of generic and specific names).
- **DTOs:** match frontend TypeScript interfaces exactly — camelCase fields, enums as uppercase `String`, `List<T>` never arrays, dates as ISO 8601 `String`, wrapper types (`Integer`, not `int`) for nullable fields.
- **Mappers:** explicit, no MapStruct. Enum→String via `.name()`. Handle dirty/unparseable source data by returning `null`/empty instead of throwing (see `CardMapper` parsing `SetCard`'s raw String fields).
- **Config over hardcoding:** external base URLs and cron schedules live in `application.yml`, injected via constructor (`@Value` on a constructor parameter, never a field).

---

## 6. Testing Rules

- No test suite exists yet (`src/test/` is empty). When adding tests:
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
- [ ] Services throw `IllegalArgumentException` for "not found" (or a subtype, e.g. `NumberFormatException` from `Long.valueOf`) — `GlobalExceptionHandler` maps it to 404. No custom exception classes.
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
- ❌ Premature abstraction / over-engineering: don't introduce a shared base class, config flag, or generic utility for a single use case. Only extract shared abstractions (like `AbstractOptcgApiClient`, `AbstractSyncScheduler`) when ≥3 near-identical implementations already exist.
- ❌ `deleteAll()` on a table shared by multiple sync jobs.
- ❌ Hardcoding an external base URL inline in more than one class — externalize to `application.yml` instead.

---

## 9. Output Expectations

Before considering a change complete, verify:

- Package placement respects §2's dependency direction (no layer violations).
- New/changed DTOs still match the frontend TypeScript contract exactly.
- OpenAPI annotations present on any new/changed controller method.
- `mvn clean compile` (and `mvn test`, once a test suite exists) passes.
- No stale references left behind after a rename (property names, method names) — grep for the old name.
- If the change alters architecture, naming conventions, or extension points described here, **update this file in the same change** — CLAUDE.md must stay in sync with the codebase it governs.

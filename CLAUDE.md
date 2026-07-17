# One Piece TCG Backend - Project Constitution

## 📋 Project Overview

This is a Spring Boot 3.4.1 backend for the One Piece Trading Card Game application. The backend provides a RESTful JSON API consumed by a React frontend located at `~/WebstormProjects/onepiecetcg`.

**Technology Stack:**
- Java 21
- Spring Boot 3.4.1
- Spring Web (REST)
- Spring Validation
- SpringDoc OpenAPI 3.0 (Swagger)
- Lombok
- PostgreSQL (via Docker Compose) + Spring Data JPA/Hibernate for `CardSet`, `SetCard` (`/api/cards` is served directly from `SetCard`, synced from optcgapi.com — no mocked `Card` entity)
- In-memory data storage (ConcurrentHashMap) for `Deck`/`Shop`

**Server Configuration:**
- Port: 3000
- Base Path: `/api`
- CORS: Enabled for `http://localhost:5173` (Vite dev server)

---

## 🏗️ Architecture Principles

### Hexagonal Architecture (Ports & Adapters)

This project follows **Hexagonal Architecture** with **Domain-Driven Design (DDD)** patterns:

```
┌─────────────────────────────────────────────────────────┐
│                     Web Layer                           │
│  Controllers, DTOs, Mappers, Exception Handlers         │
│  (Adapters - HTTP Input)                                │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│                 Application Layer                       │
│  Services (Business Logic), Models, Repository Interfaces│
│  (Application Core - Framework Independent)             │
└─────────────────────┬───────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────┐
│               Infrastructure Layer                      │
│  Repository Implementations, Data Loading               │
│  (Adapters - Persistence Output)                        │
└─────────────────────────────────────────────────────────┘
```

**Key Rules:**
1. **Application layer** is the core - it must NEVER depend on Web or Infrastructure
2. **Web layer** depends on Application (uses Services, models via Mappers)
3. **Infrastructure layer** implements Application interfaces (Repositories)
4. Business logic lives in **Services**, not Controllers
5. Controllers are thin - they only handle HTTP, delegate to Services, and transform models to DTOs

---

## 📁 Package Structure

```
pl.janda.onepiecetcg/
├── application/                    # CORE APPLICATION - Framework independent (+ bootstrap)
│   ├── OnePieceTcgApplication.java # Main Spring Boot class
│   ├── model/                      # Domain entities (POJOs)
│   │   ├── CardType.java          # Enums
│   │   ├── CardColor.java
│   │   ├── CardRarity.java
│   │   ├── Deck.java
│   │   ├── DeckCard.java
│   │   ├── Shop.java
│   │   ├── CardSet.java            # JPA entity (synced from optcgapi.com)
│   │   └── SetCard.java            # JPA entity (synced card + promo catalog from optcgapi.com, `is_promo` flag), backs `/api/cards`
│   ├── repository/                 # Repository INTERFACES (Ports)
│   │   ├── DeckRepository.java
│   │   ├── ShopRepository.java
│   │   ├── CardSetRepository.java
│   │   └── SetCardRepository.java
│   ├── client/                     # Outbound client INTERFACES (Ports)
│   │   ├── CardSetApiClient.java
│   │   ├── SetCardApiClient.java
│   │   └── PromoCardApiClient.java  # returns List<SetCard> (promo=true), not a separate model
│   └── service/                    # Business logic (Application services)
│       ├── CardService.java
│       ├── DeckService.java
│       ├── ShopService.java
│       ├── CardSetSyncService.java
│       ├── SetCardSyncService.java
│       └── PromoCardSyncService.java
│
├── infrastructure/                 # Infrastructure adapters
│   ├── persistence/                # Repository implementations
│   │   ├── InMemoryDeckRepository.java
│   │   ├── InMemoryShopRepository.java
│   │   ├── JpaCardSetRepository.java  # Spring Data JPA (PostgreSQL)
│   │   └── JpaSetCardRepository.java  # Spring Data JPA (PostgreSQL), also backs PromoCard sync (shared set_cards table)
│   ├── client/                     # External HTTP client adapters
│   │   ├── OptcgApiCardSetClient.java
│   │   ├── OptcgApiSetCardClient.java
│   │   ├── OptcgApiPromoCardClient.java # reuses OptcgSetCardResponse (identical JSON shape as /allSetCards/), builds SetCard(promo=true)
│   │   └── dto/
│   │       ├── OptcgSetResponse.java
│   │       └── OptcgSetCardResponse.java
│   └── scheduler/
│       ├── CardSetSyncScheduler.java  # Daily @Scheduled sync + startup sync
│       ├── SetCardSyncScheduler.java  # Daily @Scheduled sync + startup sync (full refresh)
│       └── PromoCardSyncScheduler.java # Daily @Scheduled sync + startup sync (full refresh)
│
└── web/                            # Web/REST layer (HTTP adapters)
    ├── config/
    │   ├── OpenApiConfig.java      # Swagger/OpenAPI setup
    │   └── CorsConfig.java         # CORS configuration
    ├── controller/                 # REST controllers
    │   ├── CardController.java
    │   ├── DeckController.java
    │   ├── ShopController.java
    │   └── GlobalExceptionHandler.java
    ├── dto/                        # Data Transfer Objects (API contracts)
    │   ├── CardDto.java
    │   ├── DeckDto.java
    │   ├── DeckCardDto.java
    │   ├── ShopDto.java
    │   ├── CreateDeckRequest.java
    │   └── UpdateDeckRequest.java
    └── mapper/                     # Entity ↔ DTO converters
        ├── CardMapper.java
        ├── DeckMapper.java
        └── ShopMapper.java
```

**Package Dependency Rules:**
```
web ───► application ◄─── infrastructure
       (allowed)    (allowed)

application ──X──► web           (FORBIDDEN)
application ──X──► infrastructure (FORBIDDEN)
infrastructure ──X──► web   (FORBIDDEN)
```

---

## 🎯 Frontend-Backend Contract

### TypeScript to Java Mapping

The frontend (`~/WebstormProjects/onepiecetcg`) defines TypeScript interfaces. Backend DTOs MUST match these exactly.

| TypeScript Type | Java DTO Field Type | Notes |
|----------------|---------------------|-------|
| `string` | `String` | |
| `string[]` | `List<String>` | Never use arrays |
| `number` | `Integer` | For cost, power, counter |
| `boolean` | `boolean` / `Boolean` | Use wrapper for nullable |
| `'Leader' \| 'Character'` | `String` (from enum) | Serialize enum as uppercase string |
| `Date` (ISO 8601) | `String` | Use `ISO_DATE_TIME` format |

### API Contract Rules

**1. URL Structure:**
- Base: `/api`
- Resources: plural nouns (`/cards`, `/decks`, `/shops`)
- ID paths: `/{id}` (e.g., `/api/cards/{id}`)

**2. HTTP Methods:**
- `GET` - Read (list or single)
- `POST` - Create (returns 201)
- `PUT` - Update (full replacement)
- `DELETE` - Delete (returns 204)

**3. Request/Response:**
- Content-Type: `application/json`
- Response format: JSON (matches DTOs)
- Error format: `{ "status": 404, "message": "...", "timestamp": "..." }`

**4. Query Parameters:**
- Use camelCase: `?costMin=3&costMax=7`
- Optional filters: all query params are `@RequestParam(required = false)`
- Lists: repeat param or comma-separated (`?color=RED&color=BLUE`)

**5. Enum Serialization:**
- Always serialize enums as **uppercase strings**: `"LEADER"`, `"RED"`, `"SR"`
- Frontend expects uppercase (TypeScript union types)

**6. Date/Time:**
- Use ISO 8601 format: `"2025-01-15T14:30:00"`
- Format with `DateTimeFormatter.ISO_DATE_TIME`

**7. Nullable Fields:**
- Frontend uses `field?: type` for optional
- Backend: use `Integer`, `String` (wrapper types, not primitives)
- Never return `undefined` - use `null` in JSON

---

## 🎯 Page-Specific API Design

### Philosophy

The backend is organized around **frontend page needs**, not just generic CRUD operations. Each major page in the frontend has corresponding API endpoints that provide exactly the data needed.

### Page-to-Controller Mapping

**HomePage** → `HomeController` (`/api/home/*`)
- Platform statistics (total cards, active decks, community size, tournaments)
- Featured updates (new sets, ban lists, announcements)
- Meta snapshot (dominant colors, trending archetypes)
- Upcoming events (tournaments, pre-releases, casual play)

**CardSearchPage** → `CardController` (`/api/cards/*`)
- Advanced search with filters (sets, colors, rarities, types, attributes, cost/power ranges)
- Card details with embedded errata and FAQ

**DeckListPage** → `DeckController` (`/api/decks/*`)
- Deck browsing and search
- Featured/popular decks endpoint
- Full CRUD for deck management

**ShopSearchPage** → `ShopController` (`/api/shops/*`)
- Shop directory with location-based search
- Shop details with contact info and opening hours

**TournamentCreatorPage** → `TournamentController` (`/api/tournaments/*`)
- Tournament CRUD (Swiss, Single Elimination, Round Robin)
- Player management and match tracking
- Standings calculation with tiebreakers (OMW%, GW%, OGW%)

### Design Principles

1. **Data Aggregation**: Endpoints aggregate data from multiple sources (e.g., `/api/home/stats` pulls from cards, decks, tournaments)
2. **Specialized Endpoints**: Create dedicated endpoints for common use cases (e.g., `/api/decks/featured` vs generic search)
3. **Complete Data**: Return complete nested objects when needed (e.g., Tournament includes full Player and Match arrays)
4. **Frontend-Driven**: API structure follows frontend needs, not database schema
5. **Minimal Round-Trips**: Design endpoints to minimize frontend API calls (include related data)

### When to Create Page-Specific Controllers

Create a new controller when:
- A new major page/feature is added to the frontend
- The page needs aggregated data from multiple sources
- The page has unique business logic (calculations, sorting, filtering)
- Generic CRUD doesn't match the page's workflow

**Example**: `HomeController` exists because HomePage needs aggregated statistics, not because "Home" is a database entity.

---

## 🧩 Layer Responsibilities

### 1. Application Layer (Core Business Logic)

**Models (`application/model/`):**
- Plain Java objects (POJOs) with Lombok annotations
- Represent business entities
- No framework dependencies (no Spring, no Jackson annotations)
- Use Java types: `LocalDateTime`, `List<T>`, enums

**Example:**
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Card {
    private String id;
    private String name;
    private CardType type;          // Enum, not String
    private List<CardColor> color;  // List of enums
    private Integer cost;           // Wrapper for nullable
    // ...
}
```

**Services (`application/service/`):**
- Contain business logic
- Annotated with `@Service`
- Inject repositories via constructor (`@RequiredArgsConstructor`)
- Work with domain models, not DTOs
- Throw `IllegalArgumentException` for not found (caught by GlobalExceptionHandler)

**Example:**
```java
@Service
@RequiredArgsConstructor
public class CardService {
    private final CardRepository cardRepository;

    public Card getCardById(String id) {
        return cardRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Card not found with id: " + id));
    }
}
```

**Repositories (`application/repository/`):**
- Interfaces only (ports)
- Define data access methods
- Return `Optional<T>` for single results, `List<T>` for collections
- Never throw exceptions - return empty Optional/List

---

### 2. Infrastructure Layer (Adapters)

**Repository Implementations (`infrastructure/persistence/`):**
- Implement repository interfaces
- Use `@Repository` annotation
- `CardSet`, `SetCard`: Spring Data JPA against PostgreSQL (e.g. `JpaSetCardRepository extends JpaRepository<SetCard, Long>, SetCardRepository`); `/api/cards` is served directly from `SetCard` (no separate mocked `Card` entity)
- `Deck`, `Shop`: still in-memory (`ConcurrentHashMap<String, T>`), not yet migrated
- Port methods that don't map to a derived-query method name (e.g. `SetCardRepository.search(...)`) are implemented as **default methods** directly on the `JpaXRepository` interface, calling `findAll()` + Java Streams — avoids Specifications/Criteria API, keeps behavior identical to the old in-memory filtering logic

**Future: Database Migration:**
- When switching to JPA/Hibernate:
  - Keep repository interfaces unchanged
  - Create new implementations (e.g., `JpaCardRepository implements CardRepository`)
  - Models may need JPA annotations (`@Entity`, `@Id`, etc.)
  - Services remain untouched (business logic independent of persistence)

---

### 3. Web Layer (REST API)

**Controllers (`web/controller/`):**
- Annotated with `@RestController` + `@RequestMapping("/api/...")`
- Thin layer - only HTTP concerns
- Inject service + mapper via constructor
- Return `ResponseEntity<DtoType>`
- Validate with `@Valid` on request bodies

**Controller Template:**
```java
@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "...")
public class CardController {
    private final CardService cardService;
    private final CardMapper cardMapper;

    @GetMapping("/{id}")
    @Operation(summary = "...")
    public ResponseEntity<CardDto> getCardById(@PathVariable String id) {
        Card card = cardService.getCardById(id);  // Service returns domain model
        return ResponseEntity.ok(cardMapper.toDto(card));  // Map to DTO
    }
}
```

**DTOs (`web/dto/`):**
- Match frontend TypeScript interfaces EXACTLY
- Use primitive wrappers (`Integer`, not `int`) for nullable fields
- Validation annotations on request DTOs (`@NotBlank`, `@NotNull`, `@Size`)
- Lombok: `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor`

**DTO Rules:**
- Field names: camelCase (matches frontend)
- Enum fields: `String` type (serialized as uppercase)
- Collections: always `List<T>`, never arrays
- Dates: `String` (ISO 8601 format)

**Example:**
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardDto {
    private String id;
    private String name;
    private String type;              // String, not CardType enum
    private List<String> color;       // List<String>, not List<CardColor>
    private Integer cost;
    // ...
}
```

**Mappers (`web/mapper/`):**
- Convert domain models ↔ DTOs
- Use `@Component` annotation
- Inject other mappers if needed (`CardMapper` in `DeckMapper`)
- Explicit mapping (no MapStruct for now - keep it simple)

**Mapping Rules:**
- Enum → String: `cardType.name()` (uppercase)
- String → Enum: `CardType.valueOf(string)`
- LocalDateTime → String: `.format(ISO_DATE_TIME)`
- List mapping: `.stream().map(...).collect(Collectors.toList())`

---

## 🎨 Code Style & Clean Code

### Naming Conventions

**Classes:**
- Domain models: `Card`, `Deck`, `Shop` (business nouns)
- Services: `CardService`, `DeckService` (noun + Service)
- Repositories: `CardRepository` (noun + Repository)
- Controllers: `CardController` (noun + Controller)
- DTOs: `CardDto`, `CreateDeckRequest`, `UpdateDeckRequest`
- Mappers: `CardMapper` (noun + Mapper)

**Methods:**
- Services: `getCardById`, `searchCards`, `createDeck`, `updateDeck`, `deleteDeck`
- Repositories: `findById`, `findAll`, `search`, `save`, `update`, `deleteById`
- Mappers: `toDto`, `toEntity`, `toDtoList`
- Controllers: match HTTP verbs (`getCardById`, `createDeck`, `updateDeck`, `deleteDeck`)

**Variables:**
- camelCase: `cardService`, `deckMapper`, `luffyLeader`
- Meaningful names: `card`, `deck`, not `c`, `d`
- Collections: plural (`cards`, `decks`, `shops`)

### Lombok Usage

**Always use:**
- `@Data` - on DTOs, domain models (generates getters, setters, equals, hashCode, toString)
- `@Builder` - on DTOs, domain models (for fluent construction)
- `@RequiredArgsConstructor` - on Services, Controllers (for dependency injection)
- `@NoArgsConstructor` + `@AllArgsConstructor` - on DTOs (for Jackson deserialization)

**Avoid:**
- `@Value` - use `record` in Java 17+ instead (not used in this project)

### Dependency Injection

- **Constructor injection only** (via `@RequiredArgsConstructor`)
- Never use `@Autowired` on fields
- Make fields `private final`

**Example:**
```java
@Service
@RequiredArgsConstructor  // Generates constructor for final fields
public class CardService {
    private final CardRepository cardRepository;  // Injected via constructor
}
```

### Error Handling

**Throw exceptions in Services:**
- `IllegalArgumentException` - for "not found" or invalid input
- Message format: `"Card not found with id: " + id`

**Catch in GlobalExceptionHandler:**
- `IllegalArgumentException` → 404 Not Found
- `MethodArgumentNotValidException` → 400 Bad Request (validation errors)
- `Exception` → 500 Internal Server Error

**Never:**
- Return `null` from services (use `Optional` in repositories)
- Use custom exception classes (keep it simple - use standard Java exceptions)

### Validation

**Request DTOs:**
- `@NotNull` - field cannot be null
- `@NotBlank` - string cannot be null, empty, or whitespace
- `@Size(min=1, max=100)` - string/collection size constraints
- `@Min(0)`, `@Max(10)` - numeric constraints

**Example:**
```java
public class CreateDeckRequest {
    @NotBlank(message = "Deck name is required")
    private String name;

    @NotNull(message = "Leader card is required")
    private CardDto leader;
}
```

**Controller:**
```java
@PostMapping
public ResponseEntity<DeckDto> createDeck(@Valid @RequestBody CreateDeckRequest request) {
    // ...
}
```

---

## 📝 OpenAPI Documentation

### Controller Annotations

**Class level:**
```java
@Tag(name = "Cards", description = "Card management and search endpoints")
```

**Method level:**
```java
@Operation(summary = "Get card by ID", description = "Returns a single card by its ID")
@ApiResponses(value = {
    @ApiResponse(responseCode = "200", description = "Card found"),
    @ApiResponse(responseCode = "404", description = "Card not found")
})
```

**Parameters:**
```java
@Parameter(description = "Card ID") @PathVariable String id
@Parameter(description = "Card name to search") @RequestParam(required = false) String name
```

### Documentation Rules

- Every controller class has `@Tag`
- Every public method has `@Operation`
- All path variables and query params have `@Parameter`
- Use `@ApiResponses` for important endpoints (POST, PUT, DELETE)
- Descriptions are concise and user-facing (not developer jargon)

---

## 🔄 Data Flow Example

**GET /api/cards/27781:**

```
1. HTTP Request
   ↓
2. CardController.getCardById("27781")
   ↓
3. cardService.getCardById("27781")           // Returns SetCard (domain model)
   ↓
4. setCardRepository.findById(27781L)         // Returns Optional<SetCard>
   ↓
5. JpaSetCardRepository (PostgreSQL lookup)
   ↓
6. SetCard object returned to Service
   ↓
7. Service returns SetCard to Controller
   ↓
8. cardMapper.toDto(setCard)                  // Convert SetCard → CardDto (safe parsing of dirty String fields)
   ↓
9. ResponseEntity.ok(cardDto)
   ↓
10. JSON serialization (Jackson)
   ↓
11. HTTP Response (200 OK, JSON body)
```

**POST /api/decks:**

```
1. HTTP Request (JSON body)
   ↓
2. Jackson deserialization → CreateDeckRequest
   ↓
3. @Valid validation
   ↓
4. DeckController.createDeck(request)
   ↓
5. deckMapper.toEntity(request)               // CreateDeckRequest → Deck
   ↓
6. deckService.createDeck(deck)
   ↓
7. deckRepository.save(deck)                  // Generates ID, sets timestamps
   ↓
8. InMemoryDeckRepository (adds to ConcurrentHashMap)
   ↓
9. Deck returned to Service
   ↓
10. Service returns Deck to Controller
   ↓
11. deckMapper.toDto(deck)                    // Deck → DeckDto
   ↓
12. ResponseEntity.status(CREATED).body(dto)
   ↓
13. HTTP Response (201 Created, JSON body with generated ID)
```

---

## 🚫 What NOT to Do

### ❌ Application Layer Violations

**DON'T:**
```java
// Domain model depending on Jackson
@JsonProperty("card_number")  // WRONG - Jackson in domain
private String cardNumber;
```

**DO:**
```java
// Keep domain clean, handle serialization in DTOs
private String cardNumber;
```

---

**DON'T:**
```java
// Service returning DTO
public CardDto getCardById(String id) {  // WRONG - returns DTO
    Card card = cardRepository.findById(id).orElseThrow(...);
    return cardMapper.toDto(card);  // Mapping in service
}
```

**DO:**
```java
// Service returns domain model
public Card getCardById(String id) {  // CORRECT - returns Card
    return cardRepository.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("Card not found..."));
}
```

---

### ❌ Controller Layer Violations

**DON'T:**
```java
// Fat controller with business logic
@GetMapping
public ResponseEntity<List<CardDto>> searchCards(@RequestParam String name) {
    List<Card> cards = cardRepository.findAll().stream()  // WRONG - logic in controller
        .filter(card -> card.getName().contains(name))
        .collect(Collectors.toList());
    return ResponseEntity.ok(cardMapper.toDtoList(cards));
}
```

**DO:**
```java
// Thin controller, delegates to service
@GetMapping
public ResponseEntity<List<CardDto>> searchCards(@RequestParam String name) {
    List<Card> cards = cardService.searchCards(name, null, null, null, null, null, null, null);
    return ResponseEntity.ok(cardMapper.toDtoList(cards));
}
```

---

### ❌ DTO Violations

**DON'T:**
```java
// DTO with enums (breaks frontend contract)
public class CardDto {
    private CardType type;  // WRONG - frontend expects string
    private List<CardColor> color;  // WRONG - frontend expects List<string>
}
```

**DO:**
```java
// DTO with strings (matches frontend)
public class CardDto {
    private String type;  // CORRECT - "LEADER", "CHARACTER", etc.
    private List<String> color;  // CORRECT - ["RED", "BLUE"]
}
```

---

### ❌ Mapping Violations

**DON'T:**
```java
// Lowercase enum serialization
dto.setType(card.getType().toString().toLowerCase());  // WRONG - "leader"
```

**DO:**
```java
// Uppercase enum serialization (matches frontend)
dto.setType(card.getType().name());  // CORRECT - "LEADER"
```

---

## 🧪 Testing Strategy (Future)

### Unit Tests

**Service Layer:**
- Test business logic in isolation
- Mock repositories with Mockito
- Test happy path + edge cases (not found, validation)

**Example:**
```java
@ExtendWith(MockitoExtension.class)
class CardServiceTest {
    @Mock
    private CardRepository cardRepository;

    @InjectMocks
    private CardService cardService;

    @Test
    void getCardById_whenExists_returnsCard() {
        // Given
        Card card = Card.builder().id("card-001").name("Luffy").build();
        when(cardRepository.findById("card-001")).thenReturn(Optional.of(card));

        // When
        Card result = cardService.getCardById("card-001");

        // Then
        assertThat(result).isEqualTo(card);
    }

    @Test
    void getCardById_whenNotExists_throwsException() {
        // Given
        when(cardRepository.findById("invalid")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> cardService.getCardById("invalid"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Card not found");
    }
}
```

### Integration Tests

**Controller Layer:**
- Use `@SpringBootTest` + `MockMvc`
- Test HTTP layer (request → response)
- Verify JSON serialization/deserialization
- Test validation

**Example:**
```java
@SpringBootTest
@AutoConfigureMockMvc
class CardControllerIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void getCardById_returnsCard() throws Exception {
        mockMvc.perform(get("/api/cards/card-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value("card-001"))
            .andExpect(jsonPath("$.name").value("Monkey.D.Luffy"))
            .andExpect(jsonPath("$.type").value("LEADER"));
    }
}
```

---

## 🔧 Development Workflow

### Adding a New Entity

**Example: Adding `Tournament` entity**

**1. Application Layer:**

```java
// application/model/Tournament.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Tournament {
    private String id;
    private String name;
    private TournamentFormat format;
    private LocalDateTime startDate;
    private List<Player> players;
}

// application/repository/TournamentRepository.java
public interface TournamentRepository {
    List<Tournament> findAll();
    Optional<Tournament> findById(String id);
    Tournament save(Tournament tournament);
}

// application/service/TournamentService.java
@Service
@RequiredArgsConstructor
public class TournamentService {
    private final TournamentRepository tournamentRepository;

    public List<Tournament> getAllTournaments() {
        return tournamentRepository.findAll();
    }
    // ... other methods
}
```

**2. Infrastructure Layer:**

```java
// infrastructure/persistence/InMemoryTournamentRepository.java
@Repository
public class InMemoryTournamentRepository implements TournamentRepository {
    private final ConcurrentHashMap<String, Tournament> tournaments = new ConcurrentHashMap<>();

    @Override
    public List<Tournament> findAll() {
        return List.copyOf(tournaments.values());
    }
    // ... implement interface
}
```

**3. Web Layer:**

```java
// web/dto/TournamentDto.java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TournamentDto {
    private String id;
    private String name;
    private String format;         // String, not enum
    private String startDate;      // ISO 8601 string
    private List<PlayerDto> players;
}

// web/mapper/TournamentMapper.java
@Component
@RequiredArgsConstructor
public class TournamentMapper {
    private final PlayerMapper playerMapper;

    public TournamentDto toDto(Tournament tournament) {
        return TournamentDto.builder()
            .id(tournament.getId())
            .name(tournament.getName())
            .format(tournament.getFormat().name())  // Enum → String
            .startDate(tournament.getStartDate().format(ISO_DATE_TIME))
            .players(tournament.getPlayers().stream()
                .map(playerMapper::toDto)
                .collect(Collectors.toList()))
            .build();
    }
}

// web/controller/TournamentController.java
@RestController
@RequestMapping("/api/tournaments")
@RequiredArgsConstructor
@Tag(name = "Tournaments", description = "Tournament management endpoints")
public class TournamentController {
    private final TournamentService tournamentService;
    private final TournamentMapper tournamentMapper;

    @GetMapping
    @Operation(summary = "Get all tournaments")
    public ResponseEntity<List<TournamentDto>> getAllTournaments() {
        List<Tournament> tournaments = tournamentService.getAllTournaments();
        return ResponseEntity.ok(tournamentMapper.toDtoList(tournaments));
    }
}
```

---

## 📚 Key Design Decisions

### Why Hexagonal Architecture?

✅ **Framework independence** - Domain logic doesn't depend on Spring
✅ **Testability** - Business logic can be tested without web/DB
✅ **Flexibility** - Easy to swap persistence (in-memory → JPA)
✅ **Clean boundaries** - Clear separation of concerns

### Why In-Memory Storage?

✅ **Fast prototyping** - No database setup required
✅ **Simple testing** - Data resets on restart
✅ **Easy migration path** - Repository interfaces stay the same when switching to JPA

### Why DTOs Separate from Domain Models?

✅ **Frontend contract stability** - API doesn't change when domain changes
✅ **Serialization control** - Enums as strings, dates as ISO 8601
✅ **Versioning** - Can support multiple API versions with different DTOs

### Why Manual Mapping (not MapStruct)?

✅ **Simplicity** - Explicit, easy to understand
✅ **No magic** - Clear transformation logic
✅ **Flexibility** - Custom mapping logic for complex cases (e.g., LocalDateTime → String)

**Note:** MapStruct can be added later if mapping becomes complex.

---

## 🎯 Critical Rules Summary

### Application Layer
- ✅ Framework-independent POJOs
- ✅ Services contain business logic
- ✅ Repositories are interfaces (ports)
- ❌ NO DTOs in domain
- ❌ NO framework annotations (except `@Service`)

### Web Layer
- ✅ Controllers are thin (delegate to services)
- ✅ DTOs match frontend TypeScript interfaces exactly
- ✅ Enums serialized as uppercase strings
- ✅ Use `ResponseEntity<T>` for all endpoints
- ❌ NO business logic in controllers

### Mappers
- ✅ Domain Model ↔ DTO conversion only
- ✅ Explicit mapping (no reflection magic)
- ✅ Handle enum ↔ string conversion
- ❌ NO business logic in mappers

### Frontend Contract
- ✅ camelCase field names
- ✅ Enums as uppercase strings
- ✅ Dates as ISO 8601 strings
- ✅ Lists, never arrays
- ✅ Nullable fields use wrapper types

---

## 🚀 Future Enhancements

### Database Integration (JPA/Hibernate)

**When switching to PostgreSQL/MySQL:**

1. Add dependencies to `pom.xml`:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
    <groupId>org.postgresql</groupId>
    <artifactId>postgresql</artifactId>
</dependency>
```

2. Annotate domain models:
```java
@Entity
@Table(name = "cards")
public class Card {
    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    private CardType type;

    @ElementCollection
    @CollectionTable(name = "card_colors")
    @Enumerated(EnumType.STRING)
    private List<CardColor> color;
    // ...
}
```

3. Create JPA repositories:
```java
@Repository
public interface JpaCardRepository extends JpaRepository<Card, String>, CardRepository {
    // Spring Data generates implementations
    // Custom queries if needed:
    @Query("SELECT c FROM Card c WHERE c.name LIKE %:name%")
    List<Card> findByNameContaining(@Param("name") String name);
}
```

4. **Services remain unchanged** - they depend on `CardRepository` interface, not implementation

**✅ Implemented for `CardSet`:** PostgreSQL (via Docker Compose) + JPA is already wired up for the `CardSet` entity (`application/model/CardSet.java`, `infrastructure/persistence/JpaCardSetRepository.java`). The `Deck`/`Shop` in-memory repositories are unaffected and can follow the same pattern when migrated.

**✅ Implemented for `/api/cards`:** the mocked `Card`/`CardRepository`/`JpaCardRepository`/`MockDataLoader` stack has been removed entirely. `/api/cards` is now served directly from `SetCard` (the same entity synced from optcgapi.com, see below), via `CardService` → `SetCardRepository` → `JpaSetCardRepository`. Key design decisions, driven by the real (occasionally dirty) scraped data:
- `SetCard.id` (surrogate `Long`) is the URL identifier (`/api/cards/{id}`), because the business key `cardSetId` (e.g. `"OP01-001"`) is **not unique** — Parallel/Promo variants share it. `cardSetId` is still exposed as `CardDto.cardNumber`.
- `CardRarity` gained a `TR` (Treasure Rare) value — present in real data but missing from the original mock enum.
- `CardMapper` safely parses `SetCard`'s raw `String` fields (`cardCost`, `cardPower`, `cardType`, `cardColor` — multi-token, e.g. `"Blue Green"`) into `CardDto`'s typed fields, returning `null`/empty on unparseable or unrecognized values instead of throwing.
- `CardDto.errata`/`faq`/`trigger` were removed — that data doesn't exist on `SetCard`.
- `CardService.getCardById(String id)` parses the id via `Long.valueOf(id)`; a non-numeric id throws `NumberFormatException` (a subclass of `IllegalArgumentException`), which `GlobalExceptionHandler` already maps to 404 — no extra validation needed.
- Since promo cards are also `SetCard` rows (`is_promo = true`, see below), `/api/cards` now returns both regular and promo cards together (4567 total, not just 3485). `CardDto`/`CardMapper` do **not** expose the `promo` flag — not part of the frontend contract yet; add it only if the frontend needs to filter/display it.

### Authentication & Authorization

- Add Spring Security
- JWT tokens for stateless auth
- User roles: ADMIN, USER
- Deck ownership (users can only edit their own decks)

### External API Integration

- Sync card data from `https://www.optcgapi.com/api/`
- Scheduled jobs (`@Scheduled`) to update card database
- Cache external API responses

**✅ Implemented for card sets:** `CardSetSyncScheduler` (`infrastructure/scheduler/`) syncs `GET https://www.optcgapi.com/api/allSets/` once on startup and daily via cron (`sets.sync.cron`, default `0 0 3 * * *`), persisting results into PostgreSQL through `CardSetSyncService` → `CardSetRepository` → `JpaCardSetRepository`.

**✅ Implemented for set cards + promo cards (shared `set_cards` table):** `SetCard` stores **both** regular set cards and promo cards in the same `set_cards` table, distinguished by a boolean `is_promo` column (`SetCard.promo`). Two independent sync jobs write into this one table:
- `SetCardSyncScheduler` (`infrastructure/scheduler/`) syncs `GET https://www.optcgapi.com/api/allSetCards/` once on startup and daily via cron (`set-cards.sync.cron`, default `0 15 3 * * *`), through `SetCardSyncService` → `SetCardRepository` → `JpaSetCardRepository`.
- `PromoCardSyncScheduler` (`infrastructure/scheduler/`) syncs `GET https://www.optcgapi.com/api/allPromos/` once on startup and daily via cron (`promo-cards.sync.cron`, default `0 30 3 * * *`), through `PromoCardSyncService` → `SetCardRepository` → `JpaSetCardRepository`. The response has the exact same JSON shape as `/allSetCards/`, so `OptcgApiPromoCardClient` reuses the existing `OptcgSetCardResponse` record for parsing (builds `SetCard` with `promo(true)`) rather than duplicating a model/DTO.

Since `card_set_id` is not unique in the source data for either feed (print/rarity variants share the same id — up to 13 variants for a single promo card in the current dataset), `SetCard` uses a surrogate `@GeneratedValue` primary key instead of upsert-by-key. Because both jobs share one table, each uses a **scoped** full-refresh instead of `deleteAll()`: `SetCardSyncService` calls `setCardRepository.deleteByPromo(false)` and `PromoCardSyncService` calls `setCardRepository.deleteByPromo(true)`, each followed by `saveAll(fetched)` in a `@Transactional` method — so one job's refresh never wipes the other's rows. `deleteByPromo(boolean)` is a Spring Data derived-query method declared on the `SetCardRepository` port with no implementation needed (same pattern as `findById`). `date_scraped` is stored as a plain `String` (not `LocalDate`) because the source data mixes date formats (`yyyy-MM-dd` and `M/d/yyyy`). `SetCard` also directly backs the `/api/cards` endpoint (see the "✅ Implemented for `/api/cards`" note above), so promo cards are included in that endpoint's results.

### Advanced Features

- Deck validation rules (50 cards, 1 leader, max 4 copies)
- Tournament API (Swiss pairing, standings calculation)
- User profiles and favorites
- Card price tracking
- Deck statistics and meta analysis

---

## 📞 Support & References

**Project Structure:**
- Frontend: `~/WebstormProjects/onepiecetcg`
- Backend: `~/WebstormProjects/onepiecetcg-backend`

**Documentation:**
- Swagger UI: http://localhost:3000/swagger-ui.html
- OpenAPI Spec: http://localhost:3000/api-docs

**Key Endpoints:**

**Homepage Data:**
- Platform Stats: `GET /api/home/stats`
- Featured Update: `GET /api/home/featured`
- Meta Snapshot: `GET /api/home/meta`
- Upcoming Events: `GET /api/home/events`

**Cards:**
- Search/List: `GET /api/cards` (with filters)
- Get by ID: `GET /api/cards/{id}`

**Decks:**
- List/Search: `GET /api/decks` (with filters)
- Featured Decks: `GET /api/decks/featured`
- Get by ID: `GET /api/decks/{id}`
- Create: `POST /api/decks`
- Update: `PUT /api/decks/{id}`
- Delete: `DELETE /api/decks/{id}`

**Shops:**
- List/Search: `GET /api/shops` (with filters)
- Get by ID: `GET /api/shops/{id}`

**Tournaments:**
- List: `GET /api/tournaments`
- Get by ID: `GET /api/tournaments/{id}`
- Create: `POST /api/tournaments`
- Update: `PUT /api/tournaments/{id}`
- Delete: `DELETE /api/tournaments/{id}`

**Development:**
```bash
# Start local PostgreSQL (required for CardSet sync)
docker compose up -d

# Compile
mvn clean compile

# Run tests
mvn test

# Start server
mvn spring-boot:run

# Package JAR
mvn package
```

---

## ✅ Checklist for Code Review

Before merging/committing, verify:

- [ ] Domain models are framework-independent (no Spring annotations except `@Service`)
- [ ] Services contain business logic, controllers are thin
- [ ] DTOs match frontend TypeScript interfaces exactly
- [ ] Enums serialized as uppercase strings
- [ ] Dates formatted as ISO 8601
- [ ] Validation annotations on request DTOs
- [ ] OpenAPI annotations on all controllers/methods
- [ ] No business logic in mappers
- [ ] Constructor injection (no field injection)
- [ ] Proper exception handling (GlobalExceptionHandler catches all)
- [ ] Tests added (if applicable)
- [ ] No code duplication
- [ ] Meaningful variable/method names

---

## 📖 Version History

**v1.1.0** (2026-07-14)
- Added page-specific controllers for frontend integration
- **HomeController**: Platform stats, featured updates, meta snapshot, events
- **TournamentController**: Full tournament management (Swiss, Single Elimination, Round Robin)
- **DeckController**: Extended with featured decks endpoint
- Page-to-controller mapping documented
- 13 total API endpoints

**v1.0.0** (2026-07-14)
- Initial backend implementation
- Hexagonal architecture with DDD
- In-memory storage (ConcurrentHashMap)
- Cards, Decks, Shops API
- OpenAPI 3.0 documentation
- Mock data loader (30 cards, 10 decks, 10 shops)

---

**Last Updated:** 2026-07-14 (v1.1.0)
**Author:** Sebastian Janda
**AI Assistant:** Claude (Anthropic)

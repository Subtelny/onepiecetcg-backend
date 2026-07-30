# Railway Deployment Guide

## Environment Variables

Railway PostgreSQL automatycznie dostarcza (przez linked service):
- `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`

Musisz ustawić ręcznie w Railway dashboard:
- `SPRING_PROFILES_ACTIVE=prod`
- `CORS_ALLOWED_ORIGINS=https://twoja-domena-frontend.com` (zmień na właściwy URL frontendu)
- `INTERNAL_API_KEY=<wygeneruj bezpieczny losowy string>`

## Zarządzanie Schematem

Schemat jest w całości tworzony automatycznie przy starcie aplikacji — **nie ma ręcznego kroku
konfiguracji schematu przy pierwszym wdrożeniu**. Składa się z dwóch części:

1. **Tabele i kolumny mapowane przez JPA** — tworzy i rozszerza Hibernate (`ddl-auto: update`).
2. **Kolumna `set_cards.card_semantic_search_vector`** (generowany `tsvector` + indeks GIN) — czego
   Hibernate nie potrafi wyrazić. Stosuje ją `SetCardSearchVectorSchemaInitializer` przy każdym
   starcie, z `src/main/resources/db/set-cards-search-vector.sql`. Skrypt używa `IF NOT EXISTS`,
   więc na już zmigrowanej bazie jest to no-op.

> Dlaczego to jest automatyczne: wcześniej ten skrypt trzeba było uruchomić ręcznie raz na każde
> środowisko. Tam, gdzie ten krok pominięto, kolumny nie było, a każde wyszukiwanie SEMANTIC kończyło
> się błędem `BadSqlGrammarException` (Postgres zgłasza brakującą kolumnę jako SQLState 42703, a Spring
> mapuje całą klasę 42 na "bad SQL grammar" — komunikat mylnie sugeruje błąd składni).

`ddl-auto` pozostaje na `update` również w profilu `prod`. `validate` wymagałoby ręcznego DDL przy
każdej zmianie encji, inaczej aplikacja nie wstałaby.

### Zmiana definicji kolumny wyszukiwania

Postgres nie ma `ALTER COLUMN` dla wyrażenia `GENERATED`, więc zmiana wyrażenia wymaga odtworzenia
kolumny:

1. Zaktualizuj `src/main/resources/db/set-cards-search-vector.sql`.
2. Uruchom `scripts/db/drop-card-semantic-search-vector.sql` na docelowej bazie.
3. Zrestartuj aplikację — initializer odtworzy kolumnę z nowej definicji.

### Zmiana encji JPA + regeneracja jOOQ

1. Zaktualizuj JPA entities.
2. Uruchom lokalnie z docker-compose — Hibernate zaktualizuje lokalny schemat, a initializer doda
   kolumnę wyszukiwania.
3. Regeneruj jOOQ sources:
   ```bash
   mvn clean generate-sources -Djooq.codegen.skip=false
   ```
4. Commituj zaktualizowane entities + generated sources.
5. Deploy — Hibernate zastosuje zmiany mapowanych kolumn na Railway automatycznie.

## Build Process

Railway automatycznie:
1. Wykrywa Maven project
2. Uruchamia `mvn clean package`
3. jOOQ codegen jest pominięty (`jooq.codegen.skip=true`)
4. Używa commitowanych sources z `src/main/generated-jooq/`
5. Startuje z `Procfile` (aktywuje prod profile)

# Railway Deployment Guide

## Environment Variables

Railway PostgreSQL automatycznie dostarcza (przez linked service):
- `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`

Musisz ustawić ręcznie w Railway dashboard:
- `SPRING_PROFILES_ACTIVE=prod`
- `CORS_ALLOWED_ORIGINS=https://twoja-domena-frontend.com` (zmień na właściwy URL frontendu)
- `INTERNAL_API_KEY=<wygeneruj bezpieczny losowy string>`

## Pierwsze Wdrożenie (Schema Setup)

1. **Tymczasowo** zmień `application-prod.yml`:
   ```yaml
   spring:
     jpa:
       hibernate:
         ddl-auto: update  # TYLKO dla pierwszego wdrożenia!
   ```

2. Commit i push do Railway - Hibernate utworzy schemat automatycznie

3. Po uruchomieniu, zweryfikuj schemat w Railway PostgreSQL console:
   - Sprawdź czy tabele istnieją: `card_sets`, `set_cards`, `set_card_effects`, `card_filter_options`

4. Zmień z powrotem na `ddl-auto: validate` w `application-prod.yml`

5. Commit i redeploy

## Zarządzanie Schematem

Przy zmianach schematu:

1. Zaktualizuj JPA entities lokalnie
2. Uruchom z docker-compose, pozwól Hibernate zaktualizować lokalny schemat
3. Regeneruj jOOQ sources:
   ```bash
   mvn clean generate-sources -Djooq.codegen.skip=false
   ```
4. Commituj zaktualizowane entities + generated sources
5. **Ręcznie** zastosuj zmiany schematu do Railway database:
   - Railway PostgreSQL console
   - Lub: `railway connect postgres` (Railway CLI)
   - Lub: `psql` z Railway `DATABASE_URL`
6. Deploy zaktualizowanej aplikacji

## Build Process

Railway automatycznie:
1. Wykrywa Maven project
2. Uruchamia `mvn clean package`
3. jOOQ codegen jest pominięty (`jooq.codegen.skip=true`)
4. Używa commitowanych sources z `src/main/generated-jooq/`
5. Startuje z `Procfile` (aktywuje prod profile)

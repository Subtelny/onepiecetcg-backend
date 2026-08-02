# Railway Deployment Guide

## Environment Variables

Railway PostgreSQL automatycznie dostarcza (przez linked service):
- `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`

Musisz ustawić ręcznie w Railway dashboard:
- `SPRING_PROFILES_ACTIVE=prod`
- `CORS_ALLOWED_ORIGINS=https://twoja-domena-frontend.com` (zmień na właściwy URL frontendu)
- `INTERNAL_API_KEY=<wygeneruj bezpieczny losowy string>`
- `MALLOC_ARENA_MAX=2` — ogranicza liczbę aren malloc glibc (domyślnie do 8 × liczba rdzeni). Każda
  arena to osobna pula, której glibc praktycznie nigdy nie zwraca systemowi, więc bez tego RSS rośnie
  wraz z liczbą wątków. Musi być zmienną środowiskową — to ustawienie glibc, nie flaga JVM, więc nie
  ma sensu w `Procfile`.

## Budżet Pamięci

Kontener ma **1 GB limitu**, a Railway rozlicza obserwowane zużycie — dlatego limity są ustawione
jawnie, w dwóch miejscach:

**Flagi JVM — `Procfile`:**

| Flaga | Powód |
|---|---|
| `-Xmx192m` | Bezwzględny limit sterty. **Celowo nie `-XX:MaxRAMPercentage`**: jeśli JVM nie odczyta limitu cgroup i wpadnie w fallback na RAM hosta, procent wyliczy się od wielogigabajtowej wartości i pogorszy sprawę. Wartość bezwzględna daje ten sam wynik w obu przypadkach. Agresywna redukcja z 256m → 192m po analizie szczytowego zużycia podczas nocnego syncu (~25MB dane + JPA overhead). |
| `-Xms96m` | Niski start, żeby RSS w bezczynności był mały. Zredukowane z 128m po optymalizacji pul aplikacyjnych. |
| `-XX:+UseSerialGC` | Znikomy narzut natywny w porównaniu z G1 (remembered sets, metadane regionów) i — ważniejsze — **oddaje pamięć systemowi po full GC**, więc RSS wraca po nocnym syncu. Dłuższe pauzy nie mają tu znaczenia. |
| `-XX:MinHeapFreeRatio=10`<br>`-XX:MaxHeapFreeRatio=30` | Wymusza agresywne oddawanie pamięci opisane wyżej. |
| `-XX:MaxMetaspaceSize=224m` | Metaspace jest domyślnie nieograniczony, a to największy podejrzany poza stertą (Hibernate + wygenerowane klasy jOOQ + springdoc + PDFBox + Jsoup). Zredukowane z 256m — UWAGA: może spowodować OOM podczas inicjalizacji Spring Boot, monitoruj logi. |
| `-XX:MaxDirectMemorySize=96m` | Limit pamięci bezpośredniej dla buforów NIO. Zredukowane z 128m po analizie rzeczywistego użycia socket buffers. |
| `-XX:+ExitOnOutOfMemoryError` | Szybki restart przez Railway zamiast wielogodzinnego dławienia się na GC. |
| `-Xlog:gc*:stdout:time,level,tags` | Pełne logowanie GC z timestampami i tagami do monitorowania szczytowego zużycia pamięci podczas nocnego syncu (03:15-03:30). Rozszerzone z `-Xlog:gc+init` dla obserwacji produkcyjnej. |

**Limity pul — `application-prod.yml`:** wątki Tomcata (20 zamiast 200), pula Hikari (5 zamiast 10)
i pula `@Async` (1 zamiast 8). Domyślne wartości są policzone na ruch o rząd wielkości większy niż ten
serwis obsługuje, a płaci się za nie w RSS — każdy wątek to własny stos, a każde połączenie Hikari to
dodatkowo osobny proces backendu Postgresa (czyli oszczędność też po stronie bazy).

**Weryfikacja po deployu:** w logu deploya sprawdź blok `gc,init` — `Heap Max Capacity` musi wynosić
192M. Jeśli wcześniej rozwiązywało się do wielu GB, to właśnie był powód wysokiego RSS. Przy
`OutOfMemoryError: Metaspace` podnoś najpierw `-XX:MaxMetaspaceSize`, potem `-Xmx`. Po realny rozkład
pamięci poza stertą dodaj tymczasowo `-XX:NativeMemoryTracking=summary -XX:+PrintNMTStatistics`
(narzut 5–10%).

**UWAGA - Agresywna konfiguracja:** Obecne limity (heap 192m, metaspace 224m, direct 96m) są zoptymalizowane
pod szczytowe zużycie nocnego syncu (~25MB dane + overhead). Monitoruj logi Railway podczas:
- Inicjalizacji Spring Boot (metaspace może być za mały → OOM przy starcie)
- Nocnego syncu 03:15-03:30 (heap może być za mały → OOM przy fetch/save)
- Buildu frontendu (backend musi być dostępny, restart = failed deploy)

**Rollback plan** jeśli wystąpi OOM:
```
# Procfile - przywróć poprzednie wartości:
-Xms128m -Xmx256m -XX:MaxMetaspaceSize=256m -XX:MaxDirectMemorySize=128m
```

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

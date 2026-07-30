-- Drops the SEMANTIC-mode search column so the next application start recreates it.
--
-- The column definition itself lives in src/main/resources/db/set-cards-search-vector.sql and is
-- applied automatically on every startup by SetCardSearchVectorSchemaInitializer, using
-- IF NOT EXISTS - so an existing column is left untouched. Postgres has no ALTER COLUMN for a
-- GENERATED expression, which makes this script the way to roll out a *changed* expression:
--
--   1. Edit src/main/resources/db/set-cards-search-vector.sql.
--   2. Run this script against the target database.
--   3. Restart the app - the initializer recreates the column from the updated definition.
--
-- Also run steps 1-3 against local Postgres before `mvn generate-sources -Djooq.codegen.skip=false`,
-- so jOOQ codegen sees the column.

DROP INDEX IF EXISTS idx_set_cards_card_semantic_search_vector;

ALTER TABLE set_cards
    DROP COLUMN IF EXISTS card_semantic_search_vector;

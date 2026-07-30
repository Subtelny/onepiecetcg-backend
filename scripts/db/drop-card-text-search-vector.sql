-- DESCRIPTION/BOTH search modes have been removed (searchIn now only supports NAME and SEMANTIC).
-- card_text_search_vector was only read by those removed modes - drop it and its index.
--
-- This repo has no migration tool (Flyway/Liquibase) - run this manually, once per environment,
-- against the target Postgres instance. Re-run `mvn generate-sources` afterward so jOOQ codegen
-- drops the corresponding generated field.

DROP INDEX IF EXISTS idx_set_cards_card_text_search_vector;

ALTER TABLE set_cards
    DROP COLUMN IF EXISTS card_text_search_vector;

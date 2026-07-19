-- Adds full-text search support for card descriptions (searchIn=DESCRIPTION/BOTH on GET /api/cards).
--
-- This repo has no migration tool (Flyway/Liquibase) - schema is created via Hibernate's
-- ddl-auto: update, which cannot express a GENERATED column or a non-default (GIN) index.
-- Run this manually, once per environment, against the target Postgres instance BEFORE
-- running `mvn generate-sources` so jOOQ codegen picks up the new column.
--
-- Idempotent: safe to re-run.

ALTER TABLE set_cards
    ADD COLUMN IF NOT EXISTS card_text_search_vector tsvector
    GENERATED ALWAYS AS (to_tsvector('simple'::regconfig, coalesce(card_text, ''))) STORED;

CREATE INDEX IF NOT EXISTS idx_set_cards_card_text_search_vector
    ON set_cards USING GIN (card_text_search_vector);

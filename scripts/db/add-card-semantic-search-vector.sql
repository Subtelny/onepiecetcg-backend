-- Adds a broader full-text search column for SEMANTIC mode (searchIn=SEMANTIC on GET /api/cards),
-- combining more fields than card_text_search_vector (DESCRIPTION/BOTH mode) does: name, type,
-- color, cost, power, counter, attribute, card set id, subtypes, and effect text.
--
-- This repo has no migration tool (Flyway/Liquibase) - schema is created via Hibernate's
-- ddl-auto: update, which cannot express a GENERATED column or a non-default (GIN) index.
-- Run this manually, once per environment, against the target Postgres instance BEFORE
-- running `mvn generate-sources` so jOOQ codegen picks up the new column.
--
-- Idempotent: safe to re-run. Drops and recreates the column/index every run since Postgres has no
-- ALTER COLUMN for a GENERATED expression - re-running after a definition change (e.g. swapping
-- set_name for card_set_id) picks up the new expression instead of being a no-op.

DROP INDEX IF EXISTS idx_set_cards_card_semantic_search_vector;

ALTER TABLE set_cards
    DROP COLUMN IF EXISTS card_semantic_search_vector;

ALTER TABLE set_cards
    ADD COLUMN card_semantic_search_vector tsvector
    GENERATED ALWAYS AS (
        to_tsvector('simple'::regconfig,
            coalesce(card_name, '') || ' ' ||
            coalesce(card_type, '') || ' ' ||
            coalesce(card_color, '') || ' ' ||
            coalesce(card_cost, '') || ' ' ||
            coalesce(card_power, '') || ' ' ||
            coalesce(counter_amount::text, '') || ' ' ||
            coalesce(attribute, '') || ' ' ||
            coalesce(card_set_id, '') || ' ' ||
            coalesce(sub_types, '') || ' ' ||
            coalesce(card_text, '')
        )
    ) STORED;

CREATE INDEX idx_set_cards_card_semantic_search_vector
    ON set_cards USING GIN (card_semantic_search_vector);

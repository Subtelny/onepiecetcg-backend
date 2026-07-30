-- Canonical definition of the full-text search column backing SEMANTIC mode (searchIn=SEMANTIC on
-- GET /api/cards), combining name, type, color, cost, power, counter, attribute, card set id,
-- subtypes and card text.
--
-- Components are weighted (setweight A/B/C/D) so ts_rank_cd (see semanticRank() in
-- JooqSetCardQueryAdapter) prioritizes a match in card_name/card_set_id (A) over sub_types (B),
-- over card_text (C), over the remaining structured metadata fields (D) - e.g. a query like
-- "arlong" ranks a card actually named "Arlong" above one that merely mentions "Arlong Pirates"
-- in its sub_types or card text.
--
-- Hibernate's ddl-auto cannot express a GENERATED column or a non-default (GIN) index, and the
-- column is deliberately not mapped on the SetCard entity, so this DDL lives outside JPA. It is
-- applied automatically on every startup by SetCardSearchVectorSchemaInitializer - this file is
-- the single source of truth for the definition; do not duplicate it in tests or in scripts/db.
--
-- Idempotent and cheap to re-run: IF NOT EXISTS makes an already-migrated database a no-op, so
-- rebuilding a stored tsvector over the whole table never happens on a normal boot.
--
-- Changing the expression below is therefore NOT picked up on its own, because Postgres has no
-- ALTER COLUMN for a GENERATED expression. To roll out a changed definition, run
-- scripts/db/drop-card-semantic-search-vector.sql against the target database and restart the
-- app - the startup initializer then recreates the column from this file.

ALTER TABLE set_cards
    ADD COLUMN IF NOT EXISTS card_semantic_search_vector tsvector
    GENERATED ALWAYS AS (
        setweight(to_tsvector('simple'::regconfig, coalesce(card_name, '') || ' ' || coalesce(card_set_id, '')), 'A') ||
        setweight(to_tsvector('simple'::regconfig, coalesce(sub_types, '')), 'B') ||
        setweight(to_tsvector('simple'::regconfig, coalesce(card_text, '')), 'C') ||
        setweight(to_tsvector('simple'::regconfig,
            coalesce(card_type, '') || ' ' ||
            coalesce(card_color, '') || ' ' ||
            coalesce(card_cost, '') || ' ' ||
            coalesce(card_power, '') || ' ' ||
            coalesce(counter_amount::text, '') || ' ' ||
            coalesce(attribute, '')
        ), 'D')
    ) STORED;

CREATE INDEX IF NOT EXISTS idx_set_cards_card_semantic_search_vector
    ON set_cards USING GIN (card_semantic_search_vector);

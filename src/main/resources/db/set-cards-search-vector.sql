
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

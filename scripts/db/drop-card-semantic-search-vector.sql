
DROP INDEX IF EXISTS idx_set_cards_card_semantic_search_vector;

ALTER TABLE set_cards
    DROP COLUMN IF EXISTS card_semantic_search_vector;

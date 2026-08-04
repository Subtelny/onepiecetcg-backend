
DROP INDEX IF EXISTS idx_set_cards_card_text_search_vector;

ALTER TABLE set_cards
    DROP COLUMN IF EXISTS card_text_search_vector;

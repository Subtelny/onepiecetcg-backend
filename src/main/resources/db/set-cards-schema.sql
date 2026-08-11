ALTER TABLE set_cards
DROP
COLUMN IF EXISTS is_representative,
    DROP
COLUMN IF EXISTS date_scraped,
    DROP
COLUMN IF EXISTS is_promo;

ALTER TABLE set_cards
    ADD COLUMN IF NOT EXISTS display_name varchar (255),
    ADD COLUMN IF NOT EXISTS source_product varchar (255);

ALTER TABLE set_cards
ALTER
COLUMN variant_index TYPE varchar(16) USING variant_index::varchar,
    ALTER
COLUMN variant_index SET DEFAULT '0';

UPDATE set_cards
SET variant_index = CASE
                        WHEN card_image_id ~ '_[pr][1-9][0-9]*$'
        THEN substring(card_image_id FROM '_([pr][1-9][0-9]*)$')
                        ELSE '0'
    END
WHERE variant_index IS DISTINCT
FROM CASE
    WHEN card_image_id ~ '_[pr][1-9][0-9]*$'
    THEN substring (card_image_id FROM '_([pr][1-9][0-9]*)$')
    ELSE '0'
END;

ALTER TABLE set_cards
    ADD COLUMN IF NOT EXISTS card_semantic_search_vector tsvector
    GENERATED ALWAYS AS (
    setweight(to_tsvector('simple'::regconfig, coalesce (card_name, '') || ' ' || coalesce (card_set_id, '')), 'A') ||
    setweight(to_tsvector('simple'::regconfig, coalesce (sub_types, '')), 'B') ||
    setweight(to_tsvector('simple'::regconfig, coalesce (card_text, '')), 'C') ||
    setweight(to_tsvector('simple'::regconfig,
    coalesce (card_type, '') || ' ' ||
    coalesce (card_color, '') || ' ' ||
    coalesce (card_cost, '') || ' ' ||
    coalesce (card_power, '') || ' ' ||
    coalesce (counter_amount::text, '') || ' ' ||
    coalesce (attribute, '')
    ), 'D')
    ) STORED;

CREATE INDEX IF NOT EXISTS idx_set_cards_card_semantic_search_vector
    ON set_cards USING GIN (card_semantic_search_vector);

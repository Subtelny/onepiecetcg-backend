ALTER TABLE set_cards
DROP
COLUMN IF EXISTS is_representative,
    DROP
COLUMN IF EXISTS date_scraped,
    DROP
COLUMN IF EXISTS is_promo;

ALTER TABLE set_cards
    ADD COLUMN IF NOT EXISTS display_name varchar (255),
    ADD COLUMN IF NOT EXISTS source_product varchar (255),
    ADD COLUMN IF NOT EXISTS card_id varchar (255);

ALTER TABLE set_cards
ALTER
COLUMN variant_index TYPE varchar(16) USING variant_index::varchar,
    ALTER
COLUMN variant_index SET DEFAULT '0';

UPDATE set_cards
SET variant_index = CASE
                        WHEN card_id ~ '_[pr][1-9][0-9]*$'
        THEN substring(card_id FROM '_([pr][1-9][0-9]*)$')
                        ELSE '0'
    END
WHERE variant_index IS DISTINCT
FROM CASE
    WHEN card_id ~ '_[pr][1-9][0-9]*$'
    THEN substring (card_id FROM '_([pr][1-9][0-9]*)$')
    ELSE '0'
END;

DELETE
FROM set_cards duplicate USING (
    SELECT id
    FROM (
        SELECT id,
               row_number() OVER (
                   PARTITION BY card_id
                   ORDER BY last_synced_at DESC NULLS LAST, id DESC
               ) AS occurrence
        FROM set_cards
        WHERE card_id IS NOT NULL
    ) ranked
    WHERE occurrence > 1
) obsolete
WHERE duplicate.id = obsolete.id;

CREATE UNIQUE INDEX IF NOT EXISTS uk_set_cards_card_id
    ON set_cards (card_id)
    WHERE card_id IS NOT NULL;

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

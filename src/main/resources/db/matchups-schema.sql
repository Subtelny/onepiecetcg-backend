SELECT pg_advisory_xact_lock(hashtext('onepiecetcg:matchups_schema'));

ALTER TABLE matchup_leaders
    ADD COLUMN IF NOT EXISTS dataset VARCHAR (255) DEFAULT '';
ALTER TABLE matchup_pairs
    ADD COLUMN IF NOT EXISTS dataset VARCHAR (255) DEFAULT '';
ALTER TABLE matchup_leader_cards
    ADD COLUMN IF NOT EXISTS dataset VARCHAR (255) DEFAULT '';

UPDATE matchup_leaders
SET dataset = COALESCE(
        (SELECT dataset FROM matchup_snapshot_info ORDER BY scraped_at DESC, id DESC LIMIT 1),
        'legacy'
    )
WHERE dataset IS NULL
   OR dataset = '';

UPDATE matchup_pairs
SET dataset = COALESCE(
        (SELECT dataset FROM matchup_snapshot_info ORDER BY scraped_at DESC, id DESC LIMIT 1),
        'legacy'
    )
WHERE dataset IS NULL
   OR dataset = '';

UPDATE matchup_leader_cards
SET dataset = COALESCE(
        (SELECT dataset FROM matchup_snapshot_info ORDER BY scraped_at DESC, id DESC LIMIT 1),
        'legacy'
    )
WHERE dataset IS NULL
   OR dataset = '';

ALTER TABLE matchup_leaders
    ALTER COLUMN dataset SET NOT NULL,
ALTER
COLUMN dataset DROP
DEFAULT;
ALTER TABLE matchup_pairs
    ALTER COLUMN dataset SET NOT NULL,
ALTER
COLUMN dataset DROP
DEFAULT;
ALTER TABLE matchup_leader_cards
    ALTER COLUMN dataset SET NOT NULL,
ALTER
COLUMN dataset DROP
DEFAULT;

ALTER TABLE matchup_leaders
DROP
CONSTRAINT IF EXISTS matchup_leaders_pkey;
ALTER TABLE matchup_leaders
    ADD CONSTRAINT matchup_leaders_pkey PRIMARY KEY (dataset, card_code);

ALTER TABLE matchup_pairs
DROP
CONSTRAINT IF EXISTS matchup_pairs_pkey;
ALTER TABLE matchup_pairs
    ADD CONSTRAINT matchup_pairs_pkey PRIMARY KEY (dataset, leader_code, opponent_code);

ALTER TABLE matchup_leader_cards
DROP
CONSTRAINT IF EXISTS matchup_leader_cards_pkey;
ALTER TABLE matchup_leader_cards
    ADD CONSTRAINT matchup_leader_cards_pkey PRIMARY KEY (dataset, leader_code, card_code);

DELETE
FROM matchup_snapshot_info older USING matchup_snapshot_info newer
WHERE LOWER (older.dataset) = LOWER (newer.dataset)
  AND (older.scraped_at
    , older.id)
    < (newer.scraped_at
    , newer.id);

CREATE UNIQUE INDEX IF NOT EXISTS matchup_snapshot_info_dataset_lower_uq
    ON matchup_snapshot_info (LOWER (dataset));

ALTER TABLE matchup_leader_cards
DROP
CONSTRAINT IF EXISTS matchup_leader_cards_category_check;

ALTER TABLE matchup_leader_cards
    ADD CONSTRAINT matchup_leader_cards_category_check
        CHECK (category IN ('EXPECTED', 'POSSIBLE_TECH', 'OBSERVED', 'TOP_DECK_ONLY'));

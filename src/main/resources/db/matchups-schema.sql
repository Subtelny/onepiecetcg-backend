SELECT pg_advisory_xact_lock(hashtext('onepiecetcg:matchup_leader_cards_category_check'));

ALTER TABLE matchup_leader_cards
DROP
CONSTRAINT IF EXISTS matchup_leader_cards_category_check;

ALTER TABLE matchup_leader_cards
    ADD CONSTRAINT matchup_leader_cards_category_check
        CHECK (category IN ('EXPECTED', 'POSSIBLE_TECH', 'OBSERVED', 'TOP_DECK_ONLY'));

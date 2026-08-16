SELECT pg_advisory_xact_lock(hashtext('onepiecetcg:cardmarket_expansion_mappings_match_type_check'));

ALTER TABLE cardmarket_expansion_mappings
DROP
CONSTRAINT IF EXISTS cardmarket_expansion_mappings_match_type_check;

ALTER TABLE cardmarket_expansion_mappings
    ADD CONSTRAINT cardmarket_expansion_mappings_match_type_check
        CHECK (match_type IN ('CARD_CODE_OVERLAP'));

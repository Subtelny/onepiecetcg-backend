-- Drops the set_card_effects table after the effect/keyword ("advanced filters") feature was
-- removed from the app (CardEffectExtractionService, SetCard.effects, EFFECT filter category).
--
-- This repo has no migration tool (Flyway/Liquibase) - schema is managed via Hibernate's
-- ddl-auto: update, which creates new tables/columns but never drops stale ones. Run this
-- manually, once per environment, against the target Postgres instance after deploying the
-- change that removed the effects feature.
--
-- Idempotent: safe to re-run.

DROP TABLE IF EXISTS set_card_effects;

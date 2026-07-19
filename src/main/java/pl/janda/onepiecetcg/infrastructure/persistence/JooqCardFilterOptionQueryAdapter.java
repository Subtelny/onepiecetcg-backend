package pl.janda.onepiecetcg.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.application.model.CardFilterOptionCategory;

import java.util.List;

import static pl.janda.onepiecetcg.infrastructure.persistence.jooq.tables.CardFilterOptions.CARD_FILTER_OPTIONS;

/**
 * Replaces CardFilterOptionService's previous findAll() + in-memory distinct/flatMap grouping
 * (see git history of CardFilterOptionService) with DB-side DISTINCT / regexp_split_to_table
 * aggregation over representative set cards, via JOOQ. See CLAUDE.md §3.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class JooqCardFilterOptionQueryAdapter {

    private final DSLContext dsl;

    void refresh() {
        var insert = dsl.insertInto(CARD_FILTER_OPTIONS,
                CARD_FILTER_OPTIONS.CATEGORY, CARD_FILTER_OPTIONS.VALUE, CARD_FILTER_OPTIONS.LABEL);
        var count = 0;

        for (var value : distinctValues("""
                SELECT DISTINCT upper(card_type) AS v FROM set_cards
                WHERE is_representative AND card_type IS NOT NULL
                """)) {
            insert.values(CardFilterOptionCategory.TYPE.name(), value, null);
            count++;
        }
        for (var value : distinctValues("""
                SELECT DISTINCT upper(t) AS v FROM set_cards, regexp_split_to_table(card_color, '\\s+') AS t
                WHERE is_representative AND card_color IS NOT NULL AND t <> ''
                """)) {
            insert.values(CardFilterOptionCategory.COLOR.name(), value, null);
            count++;
        }
        for (var value : distinctValues("""
                SELECT DISTINCT upper(rarity) AS v FROM set_cards
                WHERE is_representative AND rarity IS NOT NULL
                """)) {
            insert.values(CardFilterOptionCategory.RARITY.name(), value, null);
            count++;
        }
        for (var value : distinctValues("""
                SELECT DISTINCT upper(flat_rarity) AS v FROM set_cards
                WHERE is_representative AND flat_rarity IS NOT NULL
                """)) {
            insert.values(CardFilterOptionCategory.FLAT_RARITY.name(), value, null);
            count++;
        }
        for (var value : distinctValues("""
                SELECT DISTINCT card_cost AS v FROM set_cards
                WHERE is_representative AND card_cost ~ '^-?[0-9]+$'
                """)) {
            insert.values(CardFilterOptionCategory.COST.name(), value, null);
            count++;
        }
        for (var value : distinctValues("""
                SELECT DISTINCT t AS v FROM set_cards, regexp_split_to_table(attribute, '[\\s/]+') AS t
                WHERE is_representative AND attribute IS NOT NULL AND trim(attribute) <> ''
                  AND t <> '' AND lower(t) <> 'null'
                """)) {
            insert.values(CardFilterOptionCategory.ATTRIBUTE.name(), value, null);
            count++;
        }
        for (var value : distinctValues("""
                SELECT DISTINCT combo AS v FROM (
                    SELECT (SELECT string_agg(tok, ' & ' ORDER BY tok)
                            FROM regexp_split_to_table(attribute, '[\\s/]+') AS tok
                            WHERE tok <> '' AND lower(tok) <> 'null') AS combo
                    FROM set_cards
                    WHERE is_representative AND attribute IS NOT NULL AND trim(attribute) <> ''
                ) sub
                WHERE combo LIKE '% & %'
                """)) {
            insert.values(CardFilterOptionCategory.ATTRIBUTE_COMBO.name(), value, null);
            count++;
        }
        for (var value : distinctValues("""
                SELECT DISTINCT t AS v FROM set_cards, regexp_split_to_table(sub_types, '\\s+') AS t
                WHERE is_representative AND sub_types IS NOT NULL AND t <> ''
                """)) {
            insert.values(CardFilterOptionCategory.SUB_TYPE.name(), value, null);
            count++;
        }
        for (var value : distinctValues("""
                SELECT DISTINCT card_prefix AS v FROM set_cards
                WHERE is_representative AND card_prefix IS NOT NULL
                """)) {
            insert.values(CardFilterOptionCategory.PREFIX.name(), value, null);
            count++;
        }
        for (var value : distinctValues("""
                SELECT DISTINCT sce.effect AS v FROM set_card_effects sce
                JOIN set_cards sc ON sc.id = sce.set_card_id
                WHERE sc.is_representative
                """)) {
            insert.values(CardFilterOptionCategory.EFFECT.name(), value, null);
            count++;
        }
        for (var record : dsl.fetch("""
                SELECT DISTINCT ON (set_id) set_id, set_name FROM set_cards
                WHERE is_representative AND set_id IS NOT NULL
                ORDER BY set_id
                """)) {
            insert.values(CardFilterOptionCategory.SET.name(),
                    record.get("set_id", String.class), record.get("set_name", String.class));
            count++;
        }

        dsl.deleteFrom(CARD_FILTER_OPTIONS).execute();
        if (count > 0) {
            insert.execute();
        }
        log.info("Refreshed {} card filter option entries", count);
    }

    private List<String> distinctValues(String sql) {
        return dsl.fetch(sql).getValues("v", String.class);
    }
}

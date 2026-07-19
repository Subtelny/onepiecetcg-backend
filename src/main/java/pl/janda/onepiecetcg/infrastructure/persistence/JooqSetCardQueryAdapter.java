package pl.janda.onepiecetcg.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jooq.Condition;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.SortField;
import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.application.model.CardColor;
import pl.janda.onepiecetcg.application.model.CardRarity;
import pl.janda.onepiecetcg.application.model.CardSortField;
import pl.janda.onepiecetcg.application.model.CardType;
import pl.janda.onepiecetcg.application.model.SetCard;
import pl.janda.onepiecetcg.application.model.SortDirection;
import pl.janda.onepiecetcg.infrastructure.persistence.jooq.tables.records.SetCardsRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.jooq.impl.DSL.case_;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.concat;
import static org.jooq.impl.DSL.condition;
import static org.jooq.impl.DSL.exists;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.length;
import static org.jooq.impl.DSL.lower;
import static org.jooq.impl.DSL.or;
import static org.jooq.impl.DSL.rowNumber;
import static org.jooq.impl.DSL.selectOne;
import static org.jooq.impl.DSL.upper;
import static org.jooq.impl.DSL.when;
import static pl.janda.onepiecetcg.infrastructure.persistence.jooq.tables.SetCardEffects.SET_CARD_EFFECTS;
import static pl.janda.onepiecetcg.infrastructure.persistence.jooq.tables.SetCards.SET_CARDS;

/**
 * Pushes the SetCard search/filter/pagination/count that used to run as findAll() + Java Streams
 * (see git history of JpaSetCardRepository) down to the database via JOOQ. See CLAUDE.md §3.
 */
@Component
@RequiredArgsConstructor
@Slf4j
class JooqSetCardQueryAdapter {

    private final DSLContext dsl;

    /**
     * Replaces CardRepresentativeService's previous findAll() + in-memory grouping/saveAll with a
     * single window-function-based UPDATE. Mirrors CardRepresentativeService.CANONICAL_VARIANT_ORDER:
     * has-image first, then rarity rank (CardRarity ordinal), then shortest name, then highest id.
     */
    void recomputeRepresentative() {
        var groupKey = coalesce(SET_CARDS.CARD_SET_ID, concat(inline("id:"), SET_CARDS.ID.cast(String.class)));

        var rowNum = rowNumber().over()
                .partitionBy(groupKey)
                .orderBy(
                        field(SET_CARDS.CARD_IMAGE.isNull()).asc(),
                        rarityRank(SET_CARDS.FLAT_RARITY).asc(),
                        nameLength(SET_CARDS.CARD_NAME).asc(),
                        SET_CARDS.ID.desc()
                );

        var ranked = dsl.select(SET_CARDS.ID, rowNum.as("rn"))
                .from(SET_CARDS)
                .asTable("ranked");

        var rnField = ranked.field("rn", Integer.class);

        var updated = dsl.update(SET_CARDS)
                .set(SET_CARDS.IS_REPRESENTATIVE, field(rnField.eq(1)))
                .from(ranked)
                .where(SET_CARDS.ID.eq(ranked.field(SET_CARDS.ID)))
                .execute();

        log.info("Recomputed representative flag for {} set cards", updated);
    }

    private static Field<Integer> rarityRank(Field<String> column) {
        var values = CardRarity.values();
        var step = case_(column).when(values[0].name(), values[0].ordinal());
        for (var i = 1; i < values.length; i++) {
            step = step.when(values[i].name(), values[i].ordinal());
        }
        return step.else_(Integer.MAX_VALUE);
    }

    private static Field<Integer> nameLength(Field<String> column) {
        return when(column.isNull(), Integer.MAX_VALUE).else_(length(column));
    }

    /**
     * Builds the ORDER BY clause for search(). SET_CARDS.ID.asc() is always appended as a stable
     * tie-breaker so pagination doesn't skip/duplicate rows when the sorted field has equal values.
     */
    private static List<SortField<?>> buildOrderBy(CardSortField sortBy, SortDirection sortOrder) {
        if (sortBy == null) {
            return List.of(SET_CARDS.ID.asc());
        }
        Field<?> sortColumn = switch (sortBy) {
            case CARD_NUMBER -> SET_CARDS.CARD_SET_ID;
            case COST -> safeIntCast(SET_CARDS.CARD_COST);
            case POWER -> safeIntCast(SET_CARDS.CARD_POWER);
            case FLAT_RARITY -> rarityRank(SET_CARDS.FLAT_RARITY);
        };
        SortField<?> primary;
        if (sortOrder == SortDirection.DESC) {
            primary = sortColumn.desc();
        } else {
            primary = sortColumn.asc();
        }
        // Postgres defaults to NULLS FIRST on DESC; force NULLS LAST so cards without a value for the
        // sorted field (e.g. LEADER cards have no cost) always sink to the bottom, regardless of direction.
        return List.of(primary.nullsLast(), SET_CARDS.ID.asc());
    }

    /** Mirrors the cost/power filter cast in buildConditions() - card_cost/card_power are stored as String. */
    private static Field<Integer> safeIntCast(Field<String> column) {
        return field("CASE WHEN {0} ~ '^-?[0-9]+$' THEN {0}::int ELSE NULL END", Integer.class, column);
    }

    List<SetCard> search(
            String name,
            List<CardType> types,
            List<CardColor> colors,
            List<CardRarity> rarities,
            List<CardRarity> flatRarities,
            Integer cost,
            Integer power,
            Integer counterAmount,
            List<String> attributes,
            List<String> attributeCombos,
            String subTypes,
            List<String> prefixes,
            List<String> effects,
            CardSortField sortBy,
            SortDirection sortOrder,
            int page,
            int limit
    ) {
        var conditions = buildConditions(name, types, colors, rarities, flatRarities, cost, power, counterAmount,
                attributes, attributeCombos, subTypes, prefixes, effects);

        var records = dsl.selectFrom(SET_CARDS)
                .where(conditions)
                .orderBy(buildOrderBy(sortBy, sortOrder))
                .limit(limit)
                .offset(page * limit)
                .fetch();

        var effectsByCardId = fetchEffects(records.stream().map(SetCardsRecord::getId).toList());

        return records.stream()
                .map(r -> toSetCard(r, effectsByCardId.getOrDefault(r.getId(), List.of())))
                .toList();
    }

    long countSearch(
            String name,
            List<CardType> types,
            List<CardColor> colors,
            List<CardRarity> rarities,
            List<CardRarity> flatRarities,
            Integer cost,
            Integer power,
            Integer counterAmount,
            List<String> attributes,
            List<String> attributeCombos,
            String subTypes,
            List<String> prefixes,
            List<String> effects
    ) {
        var conditions = buildConditions(name, types, colors, rarities, flatRarities, cost, power, counterAmount,
                attributes, attributeCombos, subTypes, prefixes, effects);

        return dsl.selectCount()
                .from(SET_CARDS)
                .where(conditions)
                .fetchOne(0, long.class);
    }

    private List<Condition> buildConditions(
            String name,
            List<CardType> types,
            List<CardColor> colors,
            List<CardRarity> rarities,
            List<CardRarity> flatRarities,
            Integer cost,
            Integer power,
            Integer counterAmount,
            List<String> attributes,
            List<String> attributeCombos,
            String subTypes,
            List<String> prefixes,
            List<String> effects
    ) {
        var conditions = new ArrayList<Condition>();
        conditions.add(SET_CARDS.IS_REPRESENTATIVE.isTrue());

        if (name != null) {
            var pattern = "%" + name.toLowerCase() + "%";
            conditions.add(lower(SET_CARDS.CARD_NAME).like(pattern).or(lower(SET_CARDS.CARD_SET_ID).like(pattern)));
        }
        if (types != null && !types.isEmpty()) {
            conditions.add(upper(SET_CARDS.CARD_TYPE).in(types.stream().map(Enum::name).toList()));
        }
        if (colors != null && !colors.isEmpty()) {
            conditions.add(or(colors.stream().map(c -> wordMatch(SET_CARDS.CARD_COLOR, c.name())).toList()));
        }
        if (rarities != null && !rarities.isEmpty()) {
            conditions.add(upper(SET_CARDS.RARITY).in(rarities.stream().map(Enum::name).toList()));
        }
        if (flatRarities != null && !flatRarities.isEmpty()) {
            conditions.add(upper(SET_CARDS.FLAT_RARITY).in(flatRarities.stream().map(Enum::name).toList()));
        }
        if (cost != null) {
            conditions.add(condition("{0} ~ '^-?[0-9]+$' AND {0}::int = {1}", SET_CARDS.CARD_COST, cost));
        }
        if (power != null) {
            conditions.add(condition("{0} ~ '^-?[0-9]+$' AND {0}::int = {1}", SET_CARDS.CARD_POWER, power));
        }
        if (counterAmount != null) {
            conditions.add(SET_CARDS.COUNTER_AMOUNT.eq(counterAmount));
        }
        if (attributes != null && !attributes.isEmpty()) {
            conditions.add(or(attributes.stream().map(a -> wordMatch(SET_CARDS.ATTRIBUTE, a)).toList()));
        }
        if (attributeCombos != null && !attributeCombos.isEmpty()) {
            conditions.add(canonicalAttributeCombo(SET_CARDS.ATTRIBUTE).in(attributeCombos));
        }
        if (subTypes != null) {
            conditions.add(wordMatch(SET_CARDS.SUB_TYPES, subTypes));
        }
        if (prefixes != null && !prefixes.isEmpty()) {
            conditions.add(upper(SET_CARDS.CARD_PREFIX).in(prefixes.stream().map(String::toUpperCase).toList()));
        }
        if (effects != null && !effects.isEmpty()) {
            conditions.add(exists(
                    selectOne().from(SET_CARD_EFFECTS)
                            .where(SET_CARD_EFFECTS.SET_CARD_ID.eq(SET_CARDS.ID))
                            .and(upper(SET_CARD_EFFECTS.EFFECT).in(effects.stream().map(String::toUpperCase).toList()))
            ));
        }
        return conditions;
    }

    /**
     * Matches a value as a whole, case-insensitive token inside a space/slash-separated column
     * (mirrors the previous in-memory split + equalsIgnoreCase filtering in JpaSetCardRepository).
     */
    private static Condition wordMatch(Field<String> column, String value) {
        return condition("{0} ~* ('\\y' || {1} || '\\y')", column, value);
    }

    /**
     * Reproduces CardFilterOptionService's canonical attribute-combo form (tokens split on
     * whitespace/slash, blank/"null" tokens removed, sorted, joined with " & "), computed in SQL.
     */
    private static Field<String> canonicalAttributeCombo(Field<String> column) {
        return field(
                "(SELECT string_agg(t, ' & ' ORDER BY t) FROM regexp_split_to_table({0}, '[\\s/]+') AS t "
                        + "WHERE t <> '' AND lower(t) <> 'null')",
                String.class, column);
    }

    private Map<Long, List<String>> fetchEffects(List<Long> cardIds) {
        if (cardIds.isEmpty()) {
            return Map.of();
        }
        return dsl.selectFrom(SET_CARD_EFFECTS)
                .where(SET_CARD_EFFECTS.SET_CARD_ID.in(cardIds))
                .fetchGroups(SET_CARD_EFFECTS.SET_CARD_ID, SET_CARD_EFFECTS.EFFECT);
    }

    private static SetCard toSetCard(SetCardsRecord r, List<String> effects) {
        return SetCard.builder()
                .id(r.getId())
                .cardSetId(r.getCardSetId())
                .cardPrefix(r.getCardPrefix())
                .cardName(r.getCardName())
                .setId(r.getSetId())
                .setName(r.getSetName())
                .cardText(r.getCardText())
                .rarity(r.getRarity())
                .flatRarity(r.getFlatRarity())
                .cardColor(r.getCardColor())
                .cardType(r.getCardType())
                .life(r.getLife())
                .cardCost(r.getCardCost())
                .cardPower(r.getCardPower())
                .subTypes(r.getSubTypes())
                .counterAmount(r.getCounterAmount())
                .attribute(r.getAttribute())
                .dateScraped(r.getDateScraped())
                .cardImageId(r.getCardImageId())
                .cardImage(r.getCardImage())
                .inventoryPrice(r.getInventoryPrice())
                .marketPrice(r.getMarketPrice())
                .lastSyncedAt(r.getLastSyncedAt())
                .promo(Boolean.TRUE.equals(r.getIsPromo()))
                .representative(Boolean.TRUE.equals(r.getIsRepresentative()))
                .effects(effects)
                .build();
    }
}

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
import pl.janda.onepiecetcg.application.model.CardSearchField;
import pl.janda.onepiecetcg.application.model.CardSortField;
import pl.janda.onepiecetcg.application.model.CardType;
import pl.janda.onepiecetcg.application.model.SetCard;
import pl.janda.onepiecetcg.application.model.SortDirection;
import pl.janda.onepiecetcg.infrastructure.persistence.jooq.tables.records.SetCardsRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.jooq.impl.DSL.case_;
import static org.jooq.impl.DSL.coalesce;
import static org.jooq.impl.DSL.concat;
import static org.jooq.impl.DSL.condition;
import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.inline;
import static org.jooq.impl.DSL.length;
import static org.jooq.impl.DSL.lower;
import static org.jooq.impl.DSL.or;
import static org.jooq.impl.DSL.rowNumber;
import static org.jooq.impl.DSL.upper;
import static org.jooq.impl.DSL.when;
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

    /**
     * Casts card_cost/card_power (stored as String) to Integer where numeric, else NULL. Used for
     * ORDER BY (buildOrderBy) and for the cost IN-list filter in buildConditions(); power keeps its
     * own separate inline regex+cast condition there.
     */
    private static Field<Integer> safeIntCast(Field<String> column) {
        return field("CASE WHEN {0} ~ '^-?[0-9]+$' THEN {0}::int ELSE NULL END", Integer.class, column);
    }

    List<SetCard> search(
            String name,
            CardSearchField searchField,
            List<CardType> types,
            List<CardColor> colors,
            List<CardRarity> rarities,
            List<CardRarity> flatRarities,
            List<Integer> costs,
            Integer power,
            Integer counterAmount,
            List<String> attributes,
            List<String> attributeCombos,
            String subTypes,
            List<String> prefixes,
            CardSortField sortBy,
            SortDirection sortOrder,
            int page,
            int limit,
            boolean showAllVariants
    ) {
        var conditions = buildConditions(name, searchField, types, colors, rarities, flatRarities, costs, power, counterAmount,
                attributes, attributeCombos, subTypes, prefixes, showAllVariants);

        // SEMANTIC mode ranks by full-text relevance instead of the requested sortBy/sortOrder -
        // see the @Parameter javadoc on CardSearchRequest.sortBy.
        var orderBy = searchField == CardSearchField.SEMANTIC && name != null && !name.isBlank()
                ? List.<SortField<?>>of(semanticRank(name).desc(), SET_CARDS.ID.asc())
                : buildOrderBy(sortBy, sortOrder);

        var records = dsl.selectFrom(SET_CARDS)
                .where(conditions)
                .orderBy(orderBy)
                .limit(limit)
                .offset(page * limit)
                .fetch();

        return records.stream()
                .map(JooqSetCardQueryAdapter::toSetCard)
                .toList();
    }

    long countSearch(
            String name,
            CardSearchField searchField,
            List<CardType> types,
            List<CardColor> colors,
            List<CardRarity> rarities,
            List<CardRarity> flatRarities,
            List<Integer> costs,
            Integer power,
            Integer counterAmount,
            List<String> attributes,
            List<String> attributeCombos,
            String subTypes,
            List<String> prefixes,
            boolean showAllVariants
    ) {
        var conditions = buildConditions(name, searchField, types, colors, rarities, flatRarities, costs, power, counterAmount,
                attributes, attributeCombos, subTypes, prefixes, showAllVariants);

        return dsl.selectCount()
                .from(SET_CARDS)
                .where(conditions)
                .fetchOne(0, long.class);
    }

    private List<Condition> buildConditions(
            String name,
            CardSearchField searchField,
            List<CardType> types,
            List<CardColor> colors,
            List<CardRarity> rarities,
            List<CardRarity> flatRarities,
            List<Integer> costs,
            Integer power,
            Integer counterAmount,
            List<String> attributes,
            List<String> attributeCombos,
            String subTypes,
            List<String> prefixes,
            boolean showAllVariants
    ) {
        var conditions = new ArrayList<Condition>();
        if (!showAllVariants) {
            conditions.add(SET_CARDS.IS_REPRESENTATIVE.isTrue());
        }

        if (name != null && !name.isBlank()) {
            conditions.add(switch (searchField) {
                case NAME -> nameMatch(name);
                case SEMANTIC -> semanticMatch(name);
            });
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
        if (costs != null && !costs.isEmpty()) {
            conditions.add(safeIntCast(SET_CARDS.CARD_COST).in(costs));
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
        return conditions;
    }

    private static final Pattern REGEX_METACHARACTERS = Pattern.compile("[\\\\^$.|?*+()\\[\\]{}]");

    /**
     * Matches a value as a whole, case-insensitive token inside a space/slash-separated column
     * (mirrors the previous in-memory split + equalsIgnoreCase filtering in JpaSetCardRepository).
     * The value is regex-escaped since it becomes part of a Postgres regex pattern, not just a
     * bind parameter, so raw metacharacters (e.g. "?") would otherwise make the pattern invalid.
     * Boundaries use explicit space/slash delimiters (matching the tokenizer in
     * JooqCardFilterOptionQueryAdapter's regexp_split_to_table(attribute, '[\s/]+')) rather than
     * "\y" word boundaries, since "\y" never matches around a token with no word characters at
     * all (e.g. the literal value "?"), which would otherwise never be findable.
     */
    private static Condition wordMatch(Field<String> column, String value) {
        var escaped = REGEX_METACHARACTERS.matcher(value).replaceAll("\\\\$0");
        return condition("{0} ~* ('(^|[\\s/])' || {1} || '($|[\\s/])')", column, escaped);
    }

    private static Condition nameMatch(String name) {
        var pattern = "%" + name.toLowerCase() + "%";
        return lower(SET_CARDS.CARD_NAME).like(pattern).or(lower(SET_CARDS.CARD_SET_ID).like(pattern));
    }

    private static final Pattern QUOTED_PHRASE = Pattern.compile("(['\"])(.*?)\\1");

    private record QuotedQuery(String phrase, String remainder) {}

    /**
     * Finds the first single/double-quoted phrase anywhere in the query (e.g. 'Straw Hat' in
     * `black "Straw Hat"`) and returns it alongside the remaining text with that quoted segment
     * removed. SEMANTIC mode matches the phrase via phraseto_tsquery (exact, word-order-preserving)
     * and the remainder via plainto_tsquery (any-word-order), AND-ing both together when both are
     * present - see semanticMatch()/semanticRank(). QuotedQuery.phrase() is null if no quoted
     * segment is found, in which case the full query is used as-is (remainder == query).
     */
    private static QuotedQuery extractQuotedPhrase(String query) {
        var matcher = QUOTED_PHRASE.matcher(query);
        if (!matcher.find()) {
            return new QuotedQuery(null, query);
        }
        var remainder = (query.substring(0, matcher.start()) + " " + query.substring(matcher.end()))
                .trim().replaceAll("\\s+", " ");
        return new QuotedQuery(matcher.group(2), remainder);
    }

    /**
     * Full-text match against the generated tsvector column used by SEMANTIC mode (see
     * scripts/db/add-card-semantic-search-vector.sql) - covers name/type/color/cost/power/counter/
     * attribute/cardSetId/subTypes/effect. The 'simple' config here must match the one baked into
     * that generated column. A single/double-quoted segment anywhere in the query is matched as an
     * exact, word-order-preserving phrase, AND-ed with any remaining plain words (any-word-order
     * match) - see extractQuotedPhrase().
     */
    private static Condition semanticMatch(String query) {
        var parsed = extractQuotedPhrase(query);
        var ftsCondition = parsed.phrase() == null
                ? condition("{0} @@ plainto_tsquery('simple', {1})", SET_CARDS.CARD_SEMANTIC_SEARCH_VECTOR, query)
                : parsed.remainder().isBlank()
                        ? condition("{0} @@ phraseto_tsquery('simple', {1})", SET_CARDS.CARD_SEMANTIC_SEARCH_VECTOR, parsed.phrase())
                        : condition("{0} @@ (phraseto_tsquery('simple', {1}) && plainto_tsquery('simple', {2}))",
                                SET_CARDS.CARD_SEMANTIC_SEARCH_VECTOR, parsed.phrase(), parsed.remainder());
        return ftsCondition.or(emptyTsqueryFallback(query));
    }

    /**
     * Postgres's 'simple' FTS parser discards punctuation-only input (e.g. "?"), producing an
     * empty tsquery that can never match via "@@" regardless of column content. When that
     * happens, fall back to a literal case-insensitive substring match across the same columns
     * that feed card_semantic_search_vector, so symbol-only queries (matching e.g. the "?"
     * placeholder attribute value) are still findable in SEMANTIC mode.
     */
    private static Condition emptyTsqueryFallback(String query) {
        return condition("plainto_tsquery('simple', {0})::text = ''", query)
                .and(combinedSemanticText().containsIgnoreCase(query));
    }

    private static Field<String> combinedSemanticText() {
        return concat(
                coalesce(SET_CARDS.CARD_NAME, inline("")), inline(" "),
                coalesce(SET_CARDS.CARD_TYPE, inline("")), inline(" "),
                coalesce(SET_CARDS.CARD_COLOR, inline("")), inline(" "),
                coalesce(SET_CARDS.CARD_COST, inline("")), inline(" "),
                coalesce(SET_CARDS.CARD_POWER, inline("")), inline(" "),
                coalesce(SET_CARDS.COUNTER_AMOUNT.cast(String.class), inline("")), inline(" "),
                coalesce(SET_CARDS.ATTRIBUTE, inline("")), inline(" "),
                coalesce(SET_CARDS.CARD_SET_ID, inline("")), inline(" "),
                coalesce(SET_CARDS.SUB_TYPES, inline("")), inline(" "),
                coalesce(SET_CARDS.CARD_TEXT, inline(""))
        );
    }

    private static Field<Double> semanticRank(String query) {
        var parsed = extractQuotedPhrase(query);
        if (parsed.phrase() == null) {
            return field("ts_rank_cd({0}, plainto_tsquery('simple', {1}))", Double.class, SET_CARDS.CARD_SEMANTIC_SEARCH_VECTOR, query);
        }
        if (parsed.remainder().isBlank()) {
            return field("ts_rank_cd({0}, phraseto_tsquery('simple', {1}))", Double.class, SET_CARDS.CARD_SEMANTIC_SEARCH_VECTOR, parsed.phrase());
        }
        return field("ts_rank_cd({0}, phraseto_tsquery('simple', {1}) && plainto_tsquery('simple', {2}))", Double.class,
                SET_CARDS.CARD_SEMANTIC_SEARCH_VECTOR, parsed.phrase(), parsed.remainder());
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

    private static SetCard toSetCard(SetCardsRecord r) {
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
                .build();
    }
}

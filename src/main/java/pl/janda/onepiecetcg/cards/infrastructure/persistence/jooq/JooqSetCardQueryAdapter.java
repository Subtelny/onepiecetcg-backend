package pl.janda.onepiecetcg.cards.infrastructure.persistence.jooq;

import lombok.RequiredArgsConstructor;
import org.jooq.*;
import org.jooq.Record;
import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.cards.application.model.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import static org.jooq.impl.DSL.*;
import static pl.janda.onepiecetcg.infrastructure.persistence.jooq.tables.SetCards.SET_CARDS;


@Component
@RequiredArgsConstructor
public class JooqSetCardQueryAdapter {

    private final DSLContext dsl;

    private static final Field<String> PRICE_REFERENCE = field(name("set_cards", "price_reference"), String.class);

    private static final Field<Boolean> RELEASED = field(name("set_cards", "released"), Boolean.class);

    private static final Field<LocalDate> RELEASE_DATE = field(name("set_cards", "release_date"), LocalDate.class);


    private static List<SortField<?>> buildOrderBy(CardSortField sortBy, SortDirection sortOrder) {
        var resolvedSortBy = sortBy != null ? sortBy : CardSortField.CARD_NUMBER;
        Field<?> sortColumn = switch (resolvedSortBy) {
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


        return List.of(primary.nullsLast(), SET_CARDS.ID.asc());
    }

    private static Field<Integer> rarityRank(Field<String> column) {
        var values = CardRarity.values();
        var step = case_(column).when(values[0].name(), values[0].ordinal());
        for (var i = 1; i < values.length; i++) {
            step = step.when(values[i].name(), values[i].ordinal());
        }
        return step.else_(Integer.MAX_VALUE);
    }

    private static Field<Integer> safeIntCast(Field<String> column) {
        return field("CASE WHEN {0} ~ '^-?[0-9]+$' THEN {0}::int ELSE NULL END", Integer.class, column);
    }

    private static Condition nameMatch(String name) {
        var pattern = "%" + name.toLowerCase() + "%";
        return lower(SET_CARDS.CARD_NAME).like(pattern)
                .or(lower(SET_CARDS.DISPLAY_NAME).like(pattern))
                .or(lower(SET_CARDS.SOURCE_PRODUCT).like(pattern))
                .or(lower(SET_CARDS.CARD_SET_ID).like(pattern));
    }

    public long countSearch(
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
            boolean showAllVariants,
            boolean errataOnly
    ) {
        var conditions = buildConditions(name, searchField, types, colors, rarities, flatRarities, costs, power, counterAmount,
                attributes, attributeCombos, subTypes, prefixes, showAllVariants, errataOnly);

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
            boolean showAllVariants,
            boolean errataOnly
    ) {
        var conditions = new ArrayList<Condition>();
        if (!showAllVariants) {
            conditions.add(SET_CARDS.VARIANT_INDEX.eq(SetCard.DEFAULT_VARIANT_INDEX));
        }
        if (errataOnly) {
            conditions.add(hasErrata());
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


    private static Condition wordMatch(Field<String> column, String value) {
        var escaped = REGEX_METACHARACTERS.matcher(value).replaceAll("\\\\$0");
        return condition("{0} ~* ('(^|[\\s/,])' || {1} || '($|[\\s/,])')", column, escaped);
    }

    private static CardSummary toCardSummary(Record r) {
        return CardSummary.builder()
                .id(r.get(SET_CARDS.ID))
                .cardSetId(r.get(SET_CARDS.CARD_SET_ID))
                .cardName(r.get(SET_CARDS.CARD_NAME))
                .displayName(r.get(SET_CARDS.DISPLAY_NAME))
                .sourceProduct(r.get(SET_CARDS.SOURCE_PRODUCT))
                .released(Boolean.TRUE.equals(r.get(RELEASED)))
                .releaseDate(r.get(RELEASE_DATE))
                .flatRarity(r.get(SET_CARDS.FLAT_RARITY))
                .cardImage(r.get(SET_CARDS.CARD_IMAGE))
                .variantIndex(r.get(SET_CARDS.VARIANT_INDEX))
                .priceReference(r.get(PRICE_REFERENCE))
                .build();
    }


    private static Condition hasErrata() {
        return condition("EXISTS (SELECT 1 FROM card_errata WHERE card_code = {0})", SET_CARDS.CARD_SET_ID);
    }

    private static final Pattern QUOTED_PHRASE = Pattern.compile("(['\"])(.*?)\\1");

    private static final Pattern CARD_TYPE_KEYWORD = Pattern.compile(
            "(?i)(?<![\\p{L}\\p{N}_])(" + String.join("|", Arrays.stream(CardType.values())
                    .map(type -> Pattern.quote(type.name()))
                    .toList()) + ")(?![\\p{L}\\p{N}_])");

    private record QuotedQuery(String phrase, String remainder) {}


    private static QuotedQuery extractQuotedPhrase(String query) {
        var matcher = QUOTED_PHRASE.matcher(query);
        if (!matcher.find()) {
            return new QuotedQuery(null, query);
        }
        var remainder = (query.substring(0, matcher.start()) + " " + query.substring(matcher.end()))
                .trim().replaceAll("\\s+", " ");
        return new QuotedQuery(matcher.group(2), remainder);
    }


    private static Field<Object> prefixTsQuery(String text) {
        return field(
                "to_tsquery('simple', coalesce(" +
                        "(SELECT string_agg('''' || replace(lexeme, '''', '''''') || ''':*', ' & ') " +
                        " FROM unnest(to_tsvector('simple', {0})) AS lexeme), ''))",
                Object.class, text);
    }


    private static Condition semanticMatch(String query) {
        var parsed = extractQuotedPhrase(query);
        var ftsCondition = parsed.phrase() == null
                ? condition("{0} @@ {1}", SET_CARDS.CARD_SEMANTIC_SEARCH_VECTOR, prefixTsQuery(query))
                : parsed.remainder().isBlank()
                        ? condition("{0} @@ phraseto_tsquery('simple', {1})", SET_CARDS.CARD_SEMANTIC_SEARCH_VECTOR, parsed.phrase())
                        : condition("{0} @@ ({1} && phraseto_tsquery('simple', {2}))",
                                SET_CARDS.CARD_SEMANTIC_SEARCH_VECTOR, prefixTsQuery(parsed.remainder()), parsed.phrase());
        return ftsCondition.or(emptyTsqueryFallback(query));
    }


    private static Condition emptyTsqueryFallback(String query) {
        return condition("{0}::text = ''", prefixTsQuery(query))
                .and(combinedSemanticText().containsIgnoreCase(query));
    }

    private static Field<String> combinedSemanticText() {
        return concat(
                coalesce(SET_CARDS.DISPLAY_NAME, SET_CARDS.CARD_NAME, inline("")), inline(" "),
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
            return field("ts_rank_cd({0}, {1})", Double.class, SET_CARDS.CARD_SEMANTIC_SEARCH_VECTOR, prefixTsQuery(query));
        }
        if (parsed.remainder().isBlank()) {
            return field("ts_rank_cd({0}, phraseto_tsquery('simple', {1}))", Double.class, SET_CARDS.CARD_SEMANTIC_SEARCH_VECTOR, parsed.phrase());
        }
        return field("ts_rank_cd({0}, {1} && phraseto_tsquery('simple', {2}))", Double.class,
                SET_CARDS.CARD_SEMANTIC_SEARCH_VECTOR, prefixTsQuery(parsed.remainder()), parsed.phrase());
    }


    private static List<SortField<?>> buildSemanticOrderBy(String query) {
        var orderBy = new ArrayList<SortField<?>>();
        var cardTypeKeywords = extractCardTypeKeywords(query);
        if (!cardTypeKeywords.isEmpty()) {
            orderBy.add(when(upper(SET_CARDS.CARD_TYPE).in(cardTypeKeywords), 0).else_(1).asc());
        }
        orderBy.add(semanticRank(query).desc());
        orderBy.add(SET_CARDS.ID.asc());
        return orderBy;
    }

    private static List<String> extractCardTypeKeywords(String query) {
        var keywords = new ArrayList<String>();
        var matcher = CARD_TYPE_KEYWORD.matcher(query);
        while (matcher.find()) {
            var keyword = matcher.group(1).toUpperCase(Locale.ROOT);
            if (!keywords.contains(keyword)) {
                keywords.add(keyword);
            }
        }
        return keywords;
    }


    private static Field<String> canonicalAttributeCombo(Field<String> column) {
        return field(
                "(SELECT string_agg(trim(t), ' & ' ORDER BY trim(t)) FROM regexp_split_to_table({0}, '[\\s,/]+') AS t "
                        + "WHERE trim(t) <> '' AND lower(trim(t)) <> 'null')",
                String.class, column);
    }

    public List<CardSummary> search(
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
            boolean showAllVariants,
            boolean errataOnly
    ) {
        var conditions = buildConditions(name, searchField, types, colors, rarities, flatRarities, costs, power, counterAmount,
                attributes, attributeCombos, subTypes, prefixes, showAllVariants, errataOnly);


        var orderBy = sortBy == null && searchField == CardSearchField.SEMANTIC && name != null && !name.isBlank()
                ? buildSemanticOrderBy(name)
                : buildOrderBy(sortBy, sortOrder);


        var records = dsl.select(SET_CARDS.ID, SET_CARDS.CARD_SET_ID, SET_CARDS.CARD_NAME,
                        SET_CARDS.DISPLAY_NAME, SET_CARDS.SOURCE_PRODUCT, SET_CARDS.FLAT_RARITY,
                        SET_CARDS.CARD_IMAGE, SET_CARDS.VARIANT_INDEX, PRICE_REFERENCE, RELEASED, RELEASE_DATE)
                .from(SET_CARDS)
                .where(conditions)
                .orderBy(orderBy)
                .limit(limit)
                .offset(page * limit)
                .fetch();

        return records.stream()
                .map(JooqSetCardQueryAdapter::toCardSummary)
                .toList();
    }
}

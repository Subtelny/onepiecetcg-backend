package pl.janda.onepiecetcg.deckbuilder.web.dto;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import pl.janda.onepiecetcg.cards.application.model.CardSearchField;
import pl.janda.onepiecetcg.cards.application.model.CardSortField;
import pl.janda.onepiecetcg.cards.application.model.CardType;
import pl.janda.onepiecetcg.cards.application.model.SortDirection;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeckBuilderCardSearchRequest {

    @Parameter(description = "Card name, card number, or (SEMANTIC) a free-text description that may include inline shorthand tokens - `6c` = cost 6, `2kc` = counter 2000, `5kp` = power 5000 - stripped out and merged into costs/power/counterAmount before the remaining text is matched/ranked via full-text search (SEMANTIC mode) - the standalone keyword `errata` (whole word, case-insensitive) is also stripped out and filters results down to cards that have at least one errata entry - wrap part of the text in single or double quotes (e.g. `black 'Straw Hat'`) to require that segment as an exact, word-order-preserving phrase, AND-ed with any remaining plain words matched in any order")
    private String name;

    @Parameter(description = "Where to apply the `name` text: NAME (card name/number, default) or SEMANTIC (full-text relevance search over name/effect/type/color/cost/power/counter/attribute/cardNumber/subTypes - ranked by ts_rank - see `name`'s inline token/quoting syntax)")
    private CardSearchField searchIn;

    @Parameter(description = "Card types (LEADER, CHARACTER, EVENT, STAGE)")
    private List<CardType> types;

    @Parameter(description = "Card colors (RED, BLUE, GREEN, PURPLE, YELLOW, BLACK)")
    private List<String> color;

    @Parameter(description = "Card rarities (C, UC, R, SR, L, PR, SEC, TR)")
    private List<String> rarity;

    @Parameter(description = "Physically printed card rarities (C, UC, R, SR, L, PR, SEC, TR) - the rarity actually printed on the card, which can differ from `rarity` for promo reprints like judge packs")
    private List<String> flatRarity;

    @Parameter(description = "Accepted card costs (exact-match list, e.g. from a UI range selection expanded to individual values)")
    private List<Integer> costs;

    @Parameter(description = "Exact card power")
    private Integer power;

    @Parameter(description = "Exact counter amount")
    private Integer counterAmount;

    @Parameter(description = "Card attributes, e.g. Slash, Strike")
    private List<String> attributes;

    @Parameter(description = "Merged attribute combinations, e.g. 'Slash & Wisdom', 'Slash & Special'")
    private List<String> attributeCombos;

    @Parameter(description = "Sub-type to search for (e.g. Straw Hat Crew)")
    private String subTypes;

    @Parameter(description = "Card prefixes, e.g. ST, OP01, EB01")
    private List<String> prefixes;

    @Parameter(description = "Field to sort by (CARD_NUMBER, COST, POWER, FLAT_RARITY). Defaults to CARD_NUMBER ascending if omitted - except when searchIn=SEMANTIC and the query text is non-blank, in which case omitting sortBy orders by full-text relevance (ts_rank) instead. An explicitly provided sortBy always overrides relevance ranking, even in SEMANTIC mode with non-blank text.")
    private CardSortField sortBy;

    @Parameter(description = "Sort direction (ASC, DESC). Defaults to ASC.")
    private SortDirection sortOrder;

    @Parameter(description = "Page number, 0-indexed. Defaults to 0.")
    private Integer page;

    @Parameter(description = "Page size. Defaults to 50.")
    private Integer limit;

    @Parameter(description = "If true, returns every printed variant instead of collapsing each card to its single representative variant. Defaults to false.")
    private Boolean showAllVariants;
}

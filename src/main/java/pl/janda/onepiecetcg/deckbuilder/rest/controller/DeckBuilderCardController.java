package pl.janda.onepiecetcg.deckbuilder.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.janda.onepiecetcg.cards.application.model.*;
import pl.janda.onepiecetcg.cards.application.port.in.CardCatalogUseCase;
import pl.janda.onepiecetcg.deckbuilder.rest.dto.DeckBuilderCardDto;
import pl.janda.onepiecetcg.deckbuilder.rest.dto.DeckBuilderCardFilterOptionsDto;
import pl.janda.onepiecetcg.deckbuilder.rest.dto.DeckBuilderCardSearchRequest;
import pl.janda.onepiecetcg.deckbuilder.rest.dto.DeckBuilderCardSearchResponse;
import pl.janda.onepiecetcg.deckbuilder.rest.mapper.DeckBuilderCardMapper;
import pl.janda.onepiecetcg.pricing.application.model.PriceQuote;
import pl.janda.onepiecetcg.pricing.application.port.in.PriceQueryUseCase;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/deckbuilder/cards")
@RequiredArgsConstructor
@Tag(name = "Deck Builder - Cards", description = "Card search endpoints for the deck builder, mirroring the core card-browsing use cases")
public class DeckBuilderCardController {

    private final CardCatalogUseCase cardCatalogUseCase;

    private final PriceQueryUseCase priceQueryUseCase;

    private final DeckBuilderCardMapper deckBuilderCardMapper;

    @GetMapping
    @Operation(summary = "Get all cards or search with filters",
            description = "Returns filtered cards based on query parameters, paginated by page/limit. " +
                    "Each result is a lightweight summary (id, name, displayName, sourceProduct, cardNumber, " +
                    "flatRarity, imageUrl, variantIndex, latest source prices) - " +
                    "fetch /api/deckbuilder/cards/{id} for full card details (effect, stats, prices).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cards retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DeckBuilderCardSearchResponse.class)))
    })
    public ResponseEntity<DeckBuilderCardSearchResponse> searchCards(@ParameterObject DeckBuilderCardSearchRequest request) {
        var colors = request.getColor() != null ?
                request.getColor().stream().map(CardColor::valueOf).collect(Collectors.toList()) : null;

        var rarities = request.getRarity() != null ?
                request.getRarity().stream().map(CardRarity::valueOf).collect(Collectors.toList()) : null;

        var flatRarities = request.getFlatRarity() != null ?
                request.getFlatRarity().stream().map(CardRarity::valueOf).collect(Collectors.toList()) : null;

        PagedCards pagedCards = cardCatalogUseCase.searchCards(new CardSearchQuery(
                request.getName(),
                request.getSearchIn(),
                request.getTypes(),
                colors,
                rarities,
                flatRarities,
                request.getCosts(),
                request.getPower(),
                request.getCounterAmount(),
                request.getAttributes(),
                request.getAttributeCombos(),
                request.getSubTypes(),
                request.getPrefixes(),
                request.getSortBy(),
                request.getSortOrder(),
                request.getPage(),
                request.getLimit(),
                request.getShowAllVariants()));

        var pricesByReference = priceQueryUseCase.getLatestPricesByReferences(pagedCards.cards().stream()
                .map(CardSummary::getPriceReference)
                .toList());
        var response = DeckBuilderCardSearchResponse.builder()
                .cards(deckBuilderCardMapper.toSummaryDtoList(pagedCards.cards(), pricesByReference))
                .totalCount(pagedCards.totalCount())
                .page(pagedCards.page())
                .limit(pagedCards.limit())
                .hasMore(pagedCards.hasMore())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/filters")
    @Operation(summary = "Get all available card filters",
            description = "Returns all distinct filter values currently present in the card data: types, colors, rarities, sets, attributes, merged attribute combos, sub-types, and prefixes")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filter options retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DeckBuilderCardFilterOptionsDto.class)))
    })
    public ResponseEntity<DeckBuilderCardFilterOptionsDto> getFilterOptions() {
        CardFilterOptions options = cardCatalogUseCase.getFilterOptions();
        return ResponseEntity.ok(deckBuilderCardMapper.toFilterOptionsDto(options));
    }

    @GetMapping("/by-code")
    @Operation(summary = "Get a card variant by card code",
            description = "Returns a single card variant with its latest source prices, matching the given card code " +
                    "(e.g. OP10-009), selected by its source-derived variant index: 0 for the default print, " +
                    "pN for a parallel, or rN for a reprint")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card variant found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DeckBuilderCardDto.class))),
            @ApiResponse(responseCode = "404", description = "Card or variant index not found")
    })
    public ResponseEntity<DeckBuilderCardDto> getCardByCode(
            @Parameter(description = "Card code / card number, e.g. OP10-009") @RequestParam String cardCode,
            @Parameter(description = "Source-derived variant index: 0 (default), pN (parallel), or rN (reprint); defaults to 0") @RequestParam(required = false) String variant
    ) {
        SetCard card = cardCatalogUseCase.getVariantByCardCode(cardCode, variant);
        return ResponseEntity.ok(deckBuilderCardMapper.toDto(card, getPrices(card.getPriceReference())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get card by ID",
            description = "Returns a single card with its latest source prices by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DeckBuilderCardDto.class))),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<DeckBuilderCardDto> getCardById(
            @Parameter(description = "Card ID") @PathVariable String id
    ) {
        SetCard card = cardCatalogUseCase.getCardById(id);
        return ResponseEntity.ok(deckBuilderCardMapper.toDto(card, getPrices(card.getPriceReference())));
    }

    @GetMapping("/{id}/variants")
    @Operation(summary = "Get all variants of a card",
            description = "Returns every printed variant (different rarity/promo), including each variant's latest " +
                    "source prices, sharing the same card number as the given card")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Variants retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DeckBuilderCardDto.class))),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<List<DeckBuilderCardDto>> getCardVariants(
            @Parameter(description = "Card ID") @PathVariable String id
    ) {
        List<SetCard> variants = cardCatalogUseCase.getVariantsByCardId(id);
        var pricesByReference = priceQueryUseCase.getLatestPricesByReferences(variants.stream()
                .map(SetCard::getPriceReference)
                .toList());
        return ResponseEntity.ok(deckBuilderCardMapper.toDtoList(variants, pricesByReference));
    }

    private List<PriceQuote> getPrices(String priceReference) {
        if (priceReference == null) {
            return List.of();
        }
        var pricesByReference = priceQueryUseCase.getLatestPricesByReferences(List.of(priceReference));
        return pricesByReference.getOrDefault(priceReference, List.of());
    }
}

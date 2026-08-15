package pl.janda.onepiecetcg.cards.rest.controller;

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
import pl.janda.onepiecetcg.cards.application.port.in.CardDetailsUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.CardErrataQueryUseCase;
import pl.janda.onepiecetcg.cards.rest.dto.*;
import pl.janda.onepiecetcg.cards.rest.mapper.CardErrataMapper;
import pl.janda.onepiecetcg.cards.rest.mapper.CardMapper;
import pl.janda.onepiecetcg.pricing.application.model.PriceQuote;
import pl.janda.onepiecetcg.pricing.application.port.in.PriceQueryUseCase;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Card management and search endpoints")
public class CardController {

    private final CardCatalogUseCase cardCatalogUseCase;

    private final CardDetailsUseCase cardDetailsUseCase;

    private final CardErrataQueryUseCase cardErrataQueryUseCase;

    private final PriceQueryUseCase priceQueryUseCase;

    private final CardMapper cardMapper;

    private final CardErrataMapper cardErrataMapper;

    @GetMapping
    @Operation(summary = "Get all cards or search with filters",
            description = "Returns filtered cards based on query parameters, paginated by page/limit. " +
                    "Each result is a lightweight summary (id, name, displayName, sourceProduct, cardNumber, " +
                    "flatRarity, imageUrl, variantIndex, latest source prices) - " +
                    "variantIndex comes from the source card ID: 0 for the default print, pN for a parallel, " +
                    "or rN for a reprint (and matches /by-code's variant param) - " +
                    "fetch /api/cards/{id} for full card details (effect, stats, prices, errata).")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cards retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CardSearchResponse.class)))
    })
    public ResponseEntity<CardSearchResponse> searchCards(@ParameterObject CardSearchRequest request) {
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
        var response = CardSearchResponse.builder()
                .cards(cardMapper.toSummaryDtoList(pagedCards.cards(), pricesByReference))
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
                            schema = @Schema(implementation = CardFilterOptionsDto.class)))
    })
    public ResponseEntity<CardFilterOptionsDto> getFilterOptions() {
        CardFilterOptions options = cardCatalogUseCase.getFilterOptions();
        return ResponseEntity.ok(cardMapper.toFilterOptionsDto(options));
    }

    @GetMapping("/codes")
    @Operation(summary = "Get every distinct card code",
            description = "Returns all distinct card codes (e.g. OP10-009) in ascending order, one per card regardless of how many printed variants it has. Intended for building the frontend sitemap, which needs the complete set rather than a page of it.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card codes retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = String.class)))
    })
    public ResponseEntity<List<String>> getAllCardCodes() {
        return ResponseEntity.ok(cardCatalogUseCase.getAllCardCodes());
    }

    @GetMapping("/by-code")
    @Operation(summary = "Get a card variant by card code",
            description = "Returns a single card variant with its latest source prices, matching the given card code " +
                    "(e.g. OP10-009), selected by its source-derived variant index: 0 for the default print, " +
                    "pN for a parallel, or rN for a reprint")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card variant found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CardDto.class))),
            @ApiResponse(responseCode = "404", description = "Card or variant index not found")
    })
    public ResponseEntity<CardDto> getCardByCode(
            @Parameter(description = "Card code / card number, e.g. OP10-009") @RequestParam String cardCode,
            @Parameter(description = "Source-derived variant index: 0 (default), pN (parallel), or rN (reprint); defaults to 0") @RequestParam(required = false) String variant
    ) {
        var details = cardDetailsUseCase.getCardByCode(cardCode, variant);
        return ResponseEntity.ok(cardMapper.toDto(
                details.card(), details.errata(), details.faq(), getPrices(details.card().getPriceReference())));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get card by ID",
            description = "Returns a single card with its latest source prices by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CardDto.class))),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<CardDto> getCardById(
            @Parameter(description = "Card ID") @PathVariable String id
    ) {
        var details = cardDetailsUseCase.getCardById(id);
        return ResponseEntity.ok(cardMapper.toDto(
                details.card(), details.errata(), details.faq(), getPrices(details.card().getPriceReference())));
    }

    @GetMapping("/errata")
    @Operation(summary = "Get all card errata history",
            description = "Returns the full errata history (every correction ever issued) across all cards, including superseded entries for cards erratad more than once")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Errata list retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CardErrataDto.class)))
    })
    public ResponseEntity<List<CardErrataDto>> getAllErrata() {
        return ResponseEntity.ok(cardErrataMapper.toDtoList(cardErrataQueryUseCase.listAll()));
    }

    @GetMapping("/{id}/variants")
    @Operation(summary = "Get all variants of a card",
            description = "Returns every printed variant (different rarity/promo), including each variant's latest " +
                    "source prices, sharing the same card number as the given card")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Variants retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CardDto.class))),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<List<CardDto>> getCardVariants(
            @Parameter(description = "Card ID") @PathVariable String id
    ) {
        List<CardDetails> variants = cardDetailsUseCase.getCardVariants(id);
        var pricesByReference = priceQueryUseCase.getLatestPricesByReferences(variants.stream()
                .map(CardDetails::card)
                .map(SetCard::getPriceReference)
                .toList());
        return ResponseEntity.ok(variants.stream()
                .map(details -> cardMapper.toDto(
                        details.card(),
                        details.errata(),
                        details.faq(),
                        getPrices(details.card().getPriceReference(), pricesByReference)))
                .toList());
    }

    private List<PriceQuote> getPrices(String priceReference) {
        if (priceReference == null) {
            return List.of();
        }
        var pricesByReference = priceQueryUseCase.getLatestPricesByReferences(List.of(priceReference));
        return getPrices(priceReference, pricesByReference);
    }

    private List<PriceQuote> getPrices(
            String priceReference,
            Map<String, List<PriceQuote>> pricesByReference
    ) {
        if (priceReference == null || pricesByReference == null) {
            return List.of();
        }
        return pricesByReference.getOrDefault(priceReference, List.of());
    }
}

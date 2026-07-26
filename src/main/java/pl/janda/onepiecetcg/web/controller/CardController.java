package pl.janda.onepiecetcg.web.controller;

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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.janda.onepiecetcg.application.model.CardColor;
import pl.janda.onepiecetcg.application.model.CardFilterOptions;
import pl.janda.onepiecetcg.application.model.CardRarity;
import pl.janda.onepiecetcg.application.model.SetCard;
import pl.janda.onepiecetcg.application.service.CardErrataService;
import pl.janda.onepiecetcg.application.service.CardService;
import pl.janda.onepiecetcg.application.service.PagedCards;
import pl.janda.onepiecetcg.web.dto.CardDto;
import pl.janda.onepiecetcg.web.dto.CardErrataDto;
import pl.janda.onepiecetcg.web.dto.CardFilterOptionsDto;
import pl.janda.onepiecetcg.web.dto.CardSearchRequest;
import pl.janda.onepiecetcg.web.dto.CardSearchResponse;
import pl.janda.onepiecetcg.web.mapper.CardErrataMapper;
import pl.janda.onepiecetcg.web.mapper.CardMapper;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Card management and search endpoints")
public class CardController {

    private final CardService cardService;

    private final CardErrataService cardErrataService;

    private final CardMapper cardMapper;

    private final CardErrataMapper cardErrataMapper;

    @GetMapping
    @Operation(summary = "Get all cards or search with filters",
            description = "Returns filtered cards based on query parameters, paginated by page/limit. " +
                    "Each result is a lightweight summary (id, name, cardNumber, flatRarity, imageUrl) - " +
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

        PagedCards pagedCards = cardService.searchCards(
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
                request.getShowAllVariants());

        var response = CardSearchResponse.builder()
                .cards(cardMapper.toSummaryDtoList(pagedCards.cards()))
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
        CardFilterOptions options = cardService.getFilterOptions();
        return ResponseEntity.ok(cardMapper.toFilterOptionsDto(options));
    }

    @GetMapping("/by-code")
    @Operation(summary = "Get a card variant by card code",
            description = "Returns a single card variant matching the given card code (e.g. OP10-009), selected by a 0-based index into the canonically sorted variant list (same order as /{id}/variants; defaults to 0 = representative variant)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card variant found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CardDto.class))),
            @ApiResponse(responseCode = "404", description = "Card not found or variant index out of range")
    })
    public ResponseEntity<CardDto> getCardByCode(
            @Parameter(description = "Card code / card number, e.g. OP10-009") @RequestParam String cardCode,
            @Parameter(description = "0-based variant index; defaults to 0 (representative variant)") @RequestParam(required = false) Integer variant
    ) {
        SetCard card = cardService.getVariantByCardCode(cardCode, variant);
        var errata = cardErrataService.historyByCardCodes(List.of(card.getCardSetId())).get(card.getCardSetId());
        return ResponseEntity.ok(cardMapper.toDto(card, errata));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get card by ID",
            description = "Returns a single card by its ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Card found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CardDto.class))),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<CardDto> getCardById(
            @Parameter(description = "Card ID") @PathVariable String id
    ) {
        SetCard card = cardService.getCardById(id);
        var errata = cardErrataService.historyByCardCodes(List.of(card.getCardSetId())).get(card.getCardSetId());
        return ResponseEntity.ok(cardMapper.toDto(card, errata));
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
        return ResponseEntity.ok(cardErrataMapper.toDtoList(cardErrataService.listAll()));
    }

    @GetMapping("/{id}/variants")
    @Operation(summary = "Get all variants of a card",
            description = "Returns every printed variant (different rarity/promo) sharing the same card number as the given card")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Variants retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CardDto.class))),
            @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<List<CardDto>> getCardVariants(
            @Parameter(description = "Card ID") @PathVariable String id
    ) {
        List<SetCard> variants = cardService.getVariantsByCardId(id);
        var errataByCardCode = cardErrataService.historyByCardCodes(
                variants.stream().map(SetCard::getCardSetId).toList());
        return ResponseEntity.ok(cardMapper.toDtoList(variants, errataByCardCode));
    }
}

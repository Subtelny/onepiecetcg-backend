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
import org.springframework.web.bind.annotation.RestController;
import pl.janda.onepiecetcg.application.model.CardColor;
import pl.janda.onepiecetcg.application.model.CardFilterOptions;
import pl.janda.onepiecetcg.application.model.CardRarity;
import pl.janda.onepiecetcg.application.model.SetCard;
import pl.janda.onepiecetcg.application.service.CardService;
import pl.janda.onepiecetcg.application.service.PagedCards;
import pl.janda.onepiecetcg.web.dto.CardDto;
import pl.janda.onepiecetcg.web.dto.CardFilterOptionsDto;
import pl.janda.onepiecetcg.web.dto.CardSearchRequest;
import pl.janda.onepiecetcg.web.dto.CardSearchResponse;
import pl.janda.onepiecetcg.web.mapper.CardMapper;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Tag(name = "Cards", description = "Card management and search endpoints")
public class CardController {

    private final CardService cardService;

    private final CardMapper cardMapper;

    @GetMapping
    @Operation(summary = "Get all cards or search with filters",
            description = "Returns filtered cards based on query parameters, paginated by page/limit")
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
                request.getTypes(),
                colors,
                rarities,
                flatRarities,
                request.getCost(),
                request.getPower(),
                request.getCounterAmount(),
                request.getAttributes(),
                request.getSubTypes(),
                request.getPrefixes(),
                request.getEffects(),
                request.getPage(),
                request.getLimit());

        var response = CardSearchResponse.builder()
                .cards(cardMapper.toDtoList(pagedCards.cards()))
                .totalCount(pagedCards.totalCount())
                .page(pagedCards.page())
                .limit(pagedCards.limit())
                .hasMore(pagedCards.hasMore())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/filters")
    @Operation(summary = "Get all available card filters",
            description = "Returns all distinct filter values currently present in the card data: types, colors, rarities, sets, attributes, sub-types, prefixes, and effects")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Filter options retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CardFilterOptionsDto.class)))
    })
    public ResponseEntity<CardFilterOptionsDto> getFilterOptions() {
        CardFilterOptions options = cardService.getFilterOptions();
        return ResponseEntity.ok(cardMapper.toFilterOptionsDto(options));
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
        return ResponseEntity.ok(cardMapper.toDto(card));
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
        return ResponseEntity.ok(cardMapper.toDtoList(variants));
    }
}

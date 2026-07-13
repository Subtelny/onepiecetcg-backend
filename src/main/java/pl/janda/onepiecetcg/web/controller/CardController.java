package pl.janda.onepiecetcg.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.janda.onepiecetcg.application.model.Card;
import pl.janda.onepiecetcg.application.model.CardColor;
import pl.janda.onepiecetcg.application.model.CardRarity;
import pl.janda.onepiecetcg.application.model.CardType;
import pl.janda.onepiecetcg.application.service.CardService;
import pl.janda.onepiecetcg.web.dto.CardDto;
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
               description = "Returns all cards or filtered cards based on query parameters")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Cards retrieved successfully",
                     content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = CardDto.class)))
    })
    public ResponseEntity<List<CardDto>> searchCards(
            @Parameter(description = "Card name or card number to search")
            @RequestParam(required = false) String name,

            @Parameter(description = "Card type (LEADER, CHARACTER, EVENT, STAGE)")
            @RequestParam(required = false) CardType type,

            @Parameter(description = "Card colors (RED, BLUE, GREEN, PURPLE, YELLOW, BLACK)")
            @RequestParam(required = false) List<String> color,

            @Parameter(description = "Card rarities (C, UC, R, SR, L, PR, SEC)")
            @RequestParam(required = false) List<String> rarity,

            @Parameter(description = "Minimum cost")
            @RequestParam(required = false) Integer costMin,

            @Parameter(description = "Maximum cost")
            @RequestParam(required = false) Integer costMax,

            @Parameter(description = "Minimum power")
            @RequestParam(required = false) Integer powerMin,

            @Parameter(description = "Maximum power")
            @RequestParam(required = false) Integer powerMax
    ) {
        List<CardColor> colors = color != null ?
                color.stream().map(CardColor::valueOf).collect(Collectors.toList()) : null;

        List<CardRarity> rarities = rarity != null ?
                rarity.stream().map(CardRarity::valueOf).collect(Collectors.toList()) : null;

        List<Card> cards = cardService.searchCards(name, type, colors, rarities,
                                                   costMin, costMax, powerMin, powerMax);
        return ResponseEntity.ok(cardMapper.toDtoList(cards));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get card by ID",
               description = "Returns a single card by its ID (includes embedded errata and FAQ)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Card found",
                     content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = CardDto.class))),
        @ApiResponse(responseCode = "404", description = "Card not found")
    })
    public ResponseEntity<CardDto> getCardById(
            @Parameter(description = "Card ID") @PathVariable String id
    ) {
        Card card = cardService.getCardById(id);
        return ResponseEntity.ok(cardMapper.toDto(card));
    }
}

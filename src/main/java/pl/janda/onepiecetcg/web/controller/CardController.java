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
import pl.janda.onepiecetcg.web.dto.CardDto;
import pl.janda.onepiecetcg.web.dto.CardFilterOptionsDto;
import pl.janda.onepiecetcg.web.dto.CardSearchRequest;
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
    public ResponseEntity<List<CardDto>> searchCards(@ParameterObject CardSearchRequest request) {
        List<CardColor> colors = request.getColor() != null ?
                request.getColor().stream().map(CardColor::valueOf).collect(Collectors.toList()) : null;

        List<CardRarity> rarities = request.getRarity() != null ?
                request.getRarity().stream().map(CardRarity::valueOf).collect(Collectors.toList()) : null;

        List<SetCard> cards = cardService.searchCards(
                request.getName(),
                request.getTypes(),
                colors,
                rarities,
                request.getCost(),
                request.getPower(),
                request.getSetIds(),
                request.getCounterAmount(),
                request.getAttributes(),
                request.getSubTypes(),
                request.getPrefixes());
        return ResponseEntity.ok(cardMapper.toDtoList(cards));
    }

    @GetMapping("/filters")
    @Operation(summary = "Get all available card filters",
            description = "Returns all distinct filter values currently present in the card data: types, colors, rarities, sets, attributes, sub-types, and prefixes")
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
}

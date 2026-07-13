package pl.janda.onepiecetcg.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.janda.onepiecetcg.application.model.Deck;
import pl.janda.onepiecetcg.application.service.DeckService;
import pl.janda.onepiecetcg.web.dto.CreateDeckRequest;
import pl.janda.onepiecetcg.web.dto.DeckDto;
import pl.janda.onepiecetcg.web.dto.UpdateDeckRequest;
import pl.janda.onepiecetcg.web.mapper.DeckMapper;

import java.util.List;

@RestController
@RequestMapping("/api/decks")
@RequiredArgsConstructor
@Tag(name = "Decks", description = "Deck management endpoints")
public class DeckController {

    private final DeckService deckService;
    private final DeckMapper deckMapper;

    @GetMapping
    @Operation(summary = "Get all decks or search with filters",
               description = "Returns all decks or filtered decks based on query parameters")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Decks retrieved successfully",
                     content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = DeckDto.class)))
    })
    public ResponseEntity<List<DeckDto>> searchDecks(
            @Parameter(description = "Deck name to search")
            @RequestParam(required = false) String name,

            @Parameter(description = "Leader color")
            @RequestParam(required = false) String color,

            @Parameter(description = "Leader name")
            @RequestParam(required = false) String leader
    ) {
        List<Deck> decks = deckService.searchDecks(name, color, leader);
        return ResponseEntity.ok(deckMapper.toDtoList(decks));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get deck by ID", description = "Returns a single deck by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Deck found",
                     content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = DeckDto.class))),
        @ApiResponse(responseCode = "404", description = "Deck not found")
    })
    public ResponseEntity<DeckDto> getDeckById(
            @Parameter(description = "Deck ID") @PathVariable String id
    ) {
        Deck deck = deckService.getDeckById(id);
        return ResponseEntity.ok(deckMapper.toDto(deck));
    }

    @PostMapping
    @Operation(summary = "Create new deck", description = "Creates a new deck")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Deck created successfully",
                     content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = DeckDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    public ResponseEntity<DeckDto> createDeck(
            @Valid @RequestBody CreateDeckRequest request
    ) {
        Deck deck = deckMapper.toEntity(request);
        Deck savedDeck = deckService.createDeck(deck);
        return ResponseEntity.status(HttpStatus.CREATED).body(deckMapper.toDto(savedDeck));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update deck", description = "Updates an existing deck")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Deck updated successfully",
                     content = @Content(mediaType = "application/json",
                                      schema = @Schema(implementation = DeckDto.class))),
        @ApiResponse(responseCode = "404", description = "Deck not found")
    })
    public ResponseEntity<DeckDto> updateDeck(
            @Parameter(description = "Deck ID") @PathVariable String id,
            @RequestBody UpdateDeckRequest request
    ) {
        Deck existingDeck = deckService.getDeckById(id);
        Deck deck = deckMapper.toEntity(id, request, existingDeck);
        Deck updatedDeck = deckService.updateDeck(id, deck);
        return ResponseEntity.ok(deckMapper.toDto(updatedDeck));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete deck", description = "Deletes a deck by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Deck deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Deck not found")
    })
    public ResponseEntity<Void> deleteDeck(
            @Parameter(description = "Deck ID") @PathVariable String id
    ) {
        deckService.deleteDeck(id);
        return ResponseEntity.noContent().build();
    }
}

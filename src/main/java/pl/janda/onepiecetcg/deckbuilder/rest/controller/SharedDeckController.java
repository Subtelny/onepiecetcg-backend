package pl.janda.onepiecetcg.deckbuilder.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pl.janda.onepiecetcg.deckbuilder.application.port.in.SharedDeckUseCase;
import pl.janda.onepiecetcg.deckbuilder.rest.dto.CreateSharedDeckRequest;
import pl.janda.onepiecetcg.deckbuilder.rest.dto.SharedDeckCreatedDto;
import pl.janda.onepiecetcg.deckbuilder.rest.dto.SharedDeckDto;
import pl.janda.onepiecetcg.deckbuilder.rest.mapper.SharedDeckMapper;

import java.net.URI;

@RestController
@RequestMapping("/api/deckbuilder/shared-decks")
@RequiredArgsConstructor
@Tag(name = "Deck Builder - Shared Decks", description = "Immutable deck snapshots addressable by short public codes")
public class SharedDeckController {

    private final SharedDeckUseCase sharedDeckUseCase;

    private final SharedDeckMapper sharedDeckMapper;

    @PostMapping
    @Operation(
            summary = "Share a deck",
            description = "Persists an immutable deck snapshot and returns its short public path",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Deck name, optional leader card number, and stable card-number quantities",
                    content = @Content(schema = @Schema(implementation = CreateSharedDeckRequest.class))))
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Shared deck created",
                    content = @Content(schema = @Schema(implementation = SharedDeckCreatedDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid deck snapshot", content = @Content),
            @ApiResponse(responseCode = "404", description = "A referenced card does not exist", content = @Content)
    })
    public ResponseEntity<SharedDeckCreatedDto> createSharedDeck(
            @Valid @RequestBody CreateSharedDeckRequest request) {
        var details = sharedDeckUseCase.createSharedDeck(sharedDeckMapper.toCommand(request));
        var response = sharedDeckMapper.toCreatedDto(details);
        var location = URI.create("/api/deckbuilder/shared-decks/" + response.getCode());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{code}")
    @Operation(summary = "Get a shared deck", description = "Resolves a shared snapshot against the current representative card catalog")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Shared deck found",
                    content = @Content(schema = @Schema(implementation = SharedDeckDto.class))),
            @ApiResponse(responseCode = "404", description = "Shared deck not found", content = @Content)
    })
    public ResponseEntity<SharedDeckDto> getSharedDeck(
            @Parameter(description = "Case-sensitive 10-character share code", required = true)
            @PathVariable String code) {
        var details = sharedDeckUseCase.getSharedDeck(code);
        return ResponseEntity.ok(sharedDeckMapper.toDto(details));
    }
}

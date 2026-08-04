package pl.janda.onepiecetcg.cards.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.janda.onepiecetcg.cards.application.port.in.*;
import pl.janda.onepiecetcg.cards.rest.dto.SyncResultDto;

@RestController
@RequestMapping("/api/internal/sync")
@RequiredArgsConstructor
@Tag(name = "Internal Sync", description = "Dev-only endpoints to manually trigger data sync jobs. Requires X-API-Key header.")
@SecurityRequirement(name = "ApiKeyAuth")
public class InternalSyncController {

    private final CardSetSyncUseCase cardSetSyncUseCase;

    private final SetCardSyncUseCase setCardSyncUseCase;

    private final CardErrataSyncUseCase cardErrataSyncUseCase;

    private final CardFaqSyncUseCase cardFaqSyncUseCase;

    private final CardmarketPriceSyncUseCase cardmarketPriceSyncUseCase;

    @PostMapping("/card-sets")
    @Operation(summary = "Manually sync card sets",
            description = "Loads card sets from onepiece_card_sets and persists new or changed sets.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sync completed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid API key")
    })
    public ResponseEntity<SyncResultDto> syncCardSets() {
        var newSetsFound = cardSetSyncUseCase.syncCardSets();
        var message = newSetsFound ? "New card sets synced" : "No new card sets detected";
        return ResponseEntity.ok(new SyncResultDto(newSetsFound, message));
    }

    @PostMapping("/set-cards")
    @Operation(summary = "Manually sync set cards",
            description = "Triggers an async set-cards sync from onepiece_cards in a separate thread, " +
                    "then recomputes representative flags and refreshes filter options. Returns immediately.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sync triggered successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid API key")
    })
    public ResponseEntity<SyncResultDto> syncSetCards() {
        setCardSyncUseCase.syncSetCardsAsync();
        return ResponseEntity.ok(new SyncResultDto(true, "Set cards sync triggered in background"));
    }

    @PostMapping("/card-errata")
    @Operation(summary = "Manually sync card errata",
            description = "Re-scrapes the official errata list from en.onepiece-cardgame.com and replaces the stored errata history.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sync completed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid API key")
    })
    public ResponseEntity<SyncResultDto> syncCardErrata() {
        cardErrataSyncUseCase.syncErrata();
        return ResponseEntity.ok(new SyncResultDto(true, "Card errata sync completed"));
    }

    @PostMapping("/card-faq")
    @Operation(summary = "Manually sync card FAQ",
            description = "Re-checks the FAQ listing on en.onepiece-cardgame.com and re-downloads/parses the PDF " +
                    "for any card set whose published date has changed, replacing that set's stored FAQ entries.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sync completed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid API key")
    })
    public ResponseEntity<SyncResultDto> syncCardFaq() {
        cardFaqSyncUseCase.syncFaq();
        return ResponseEntity.ok(new SyncResultDto(true, "Card FAQ sync completed"));
    }

    @PostMapping("/cardmarket-prices")
    @Operation(summary = "Manually sync Cardmarket prices",
            description = "Downloads Cardmarket's public One Piece product catalog and EUR price guide, " +
                    "then appends a historical snapshot grouped by Bandai card code. An already stored " +
                    "daily price-guide publication is skipped.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sync completed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid API key")
    })
    public ResponseEntity<SyncResultDto> syncCardmarketPrices() {
        cardmarketPriceSyncUseCase.syncPrices();
        return ResponseEntity.ok(new SyncResultDto(true, "Cardmarket price sync completed"));
    }
}

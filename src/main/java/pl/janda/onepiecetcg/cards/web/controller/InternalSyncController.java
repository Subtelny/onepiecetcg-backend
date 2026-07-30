package pl.janda.onepiecetcg.cards.web.controller;

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
import pl.janda.onepiecetcg.cards.application.service.CardErrataSyncService;
import pl.janda.onepiecetcg.cards.application.service.CardFaqSyncService;
import pl.janda.onepiecetcg.cards.application.service.CardSetSyncService;
import pl.janda.onepiecetcg.cards.application.service.SetCardSyncService;
import pl.janda.onepiecetcg.cards.web.dto.SyncResultDto;

@RestController
@RequestMapping("/api/internal/sync")
@RequiredArgsConstructor
@Tag(name = "Internal Sync", description = "Dev-only endpoints to manually trigger OPTCG sync jobs. Requires X-API-Key header.")
@SecurityRequirement(name = "ApiKeyAuth")
public class InternalSyncController {

    private final CardSetSyncService cardSetSyncService;

    private final SetCardSyncService setCardSyncService;

    private final CardErrataSyncService cardErrataSyncService;

    private final CardFaqSyncService cardFaqSyncService;

    @PostMapping("/card-sets")
    @Operation(summary = "Manually sync card sets",
            description = "Fetches card sets from optcgapi.com and persists any newly detected sets.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sync completed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid API key")
    })
    public ResponseEntity<SyncResultDto> syncCardSets() {
        var newSetsFound = cardSetSyncService.syncCardSets();
        var message = newSetsFound ? "New card sets synced" : "No new card sets detected";
        return ResponseEntity.ok(new SyncResultDto(newSetsFound, message));
    }

    @PostMapping("/set-cards")
    @Operation(summary = "Manually sync set cards",
            description = "Triggers an async set-cards sync from optcgapi.com in a separate thread, bypassing the new-card-set diff gate, " +
                    "then recomputes representative flags and refreshes filter options. Returns immediately.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sync triggered successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid API key")
    })
    public ResponseEntity<SyncResultDto> syncSetCards() {
        setCardSyncService.syncSetCardsAsync(true);
        return ResponseEntity.ok(new SyncResultDto(true, "Set cards sync triggered in background (forced)"));
    }

    @PostMapping("/card-errata")
    @Operation(summary = "Manually sync card errata",
            description = "Re-scrapes the official errata list from en.onepiece-cardgame.com and replaces the stored errata history.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sync completed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid API key")
    })
    public ResponseEntity<SyncResultDto> syncCardErrata() {
        cardErrataSyncService.syncErrata();
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
        cardFaqSyncService.syncFaq();
        return ResponseEntity.ok(new SyncResultDto(true, "Card FAQ sync completed"));
    }
}

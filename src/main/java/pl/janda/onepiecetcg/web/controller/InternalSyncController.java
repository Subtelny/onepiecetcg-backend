package pl.janda.onepiecetcg.web.controller;

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
import pl.janda.onepiecetcg.application.service.CardSetSyncService;
import pl.janda.onepiecetcg.application.service.SetCardSyncService;
import pl.janda.onepiecetcg.web.dto.SyncResultDto;

@RestController
@RequestMapping("/api/internal/sync")
@RequiredArgsConstructor
@Tag(name = "Internal Sync", description = "Dev-only endpoints to manually trigger OPTCG sync jobs. Requires X-API-Key header.")
@SecurityRequirement(name = "ApiKeyAuth")
public class InternalSyncController {

    private final CardSetSyncService cardSetSyncService;

    private final SetCardSyncService setCardSyncService;

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
            description = "Forces a full set-cards sync from optcgapi.com, bypassing the new-card-set diff gate, " +
                    "then recomputes representative flags and refreshes filter options.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sync completed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid API key")
    })
    public ResponseEntity<SyncResultDto> syncSetCards() {
        setCardSyncService.syncSetCards(true);
        return ResponseEntity.ok(new SyncResultDto(true, "Set cards sync completed (forced)"));
    }
}

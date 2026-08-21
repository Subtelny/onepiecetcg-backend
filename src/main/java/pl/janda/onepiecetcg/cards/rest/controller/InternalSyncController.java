package pl.janda.onepiecetcg.cards.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.janda.onepiecetcg.cards.application.port.in.CardErrataSyncUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.CardFaqSyncUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.CardSetSyncUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.SetCardSyncUseCase;
import pl.janda.onepiecetcg.cards.rest.dto.SyncResultDto;
import pl.janda.onepiecetcg.matchups.application.port.in.MatchupSyncUseCase;

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

    private final MatchupSyncUseCase matchupSyncUseCase;

    @PostMapping("/card-sets")
    @Operation(summary = "Manually sync card sets",
            description = "Loads released sets from onepiece_card_sets and future leaked sets from " +
                    "cardkaizoku_card_sets, with official data taking precedence.")
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
            description = "Triggers an async set-cards sync from official and future-leak source tables, " +
                    "derives variant indexes from source card IDs and refreshes filter options. Returns immediately.")
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

    @PostMapping("/matchups")
    @Operation(summary = "Manually sync matchups",
            description = "Loads the latest snapshot of every dataset from " +
                    "tcgmatchmaking_matchup_snapshots/tcgmatchmaking_leader_stats/" +
                    "tcgmatchmaking_matchups/tcgmatchmaking_decklists, normalizes and merges dirty leader/opponent " +
                    "codes, derives game-weighted expected cards and possible techs using an adaptive per-leader " +
                    "win-rate cohort, enriches with card data, and replaces only the corresponding dataset in " +
                    "matchup_snapshot_info/matchup_leaders/matchup_pairs/matchup_leader_cards.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sync completed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid API key")
    })
    public ResponseEntity<SyncResultDto> syncMatchups() {
        var synced = matchupSyncUseCase.syncMatchups();
        var message = synced ? "Matchups synced" : "No matchup dataset required an update";
        return ResponseEntity.ok(new SyncResultDto(synced, message));
    }

    @PostMapping("/matchups/recalculate")
    @Operation(summary = "Force matchup dataset recalculation",
            description = "Rebuilds one matchup dataset from its latest raw snapshot even when that snapshot was " +
                    "already processed. Use this after the card catalog gains cards that were missing during the " +
                    "original matchup sync.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Dataset recalculated"),
            @ApiResponse(responseCode = "400", description = "Dataset parameter is missing"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid API key"),
            @ApiResponse(responseCode = "404", description = "Dataset not found")
    })
    public ResponseEntity<SyncResultDto> recalculateMatchups(
            @Parameter(description = "Dataset name, matched case-insensitively", example = "Special_Queue", required = true)
            @RequestParam String dataset
    ) {
        matchupSyncUseCase.recalculateMatchups(dataset);
        return ResponseEntity.ok(new SyncResultDto(true, "Matchup dataset recalculated: " + dataset));
    }
}

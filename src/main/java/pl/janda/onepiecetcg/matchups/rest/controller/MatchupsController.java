package pl.janda.onepiecetcg.matchups.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import pl.janda.onepiecetcg.matchups.application.model.MatchupSnapshotInfo;
import pl.janda.onepiecetcg.matchups.application.port.in.MatchupsQueryUseCase;
import pl.janda.onepiecetcg.matchups.rest.dto.LeaderMatchupsResponseDto;
import pl.janda.onepiecetcg.matchups.rest.dto.MatchupsOverviewResponseDto;
import pl.janda.onepiecetcg.matchups.rest.dto.MatchupsResponseDto;
import pl.janda.onepiecetcg.matchups.rest.mapper.MatchupsMapper;

import java.time.Duration;

@RestController
@RequestMapping("/api/matchups")
@RequiredArgsConstructor
@Tag(name = "Matchups", description = "Leader popularity and head-to-head matchup statistics")
public class MatchupsController {

    private static final CacheControl MATCHUPS_CACHE = CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic();
    private static final String ETAG_VERSION = "v1";

    private final MatchupsQueryUseCase matchupsQueryUseCase;

    private final MatchupsMapper matchupsMapper;

    @GetMapping
    @Operation(summary = "Get the current matchups overview",
            description = "Returns the most recently synced snapshot metadata, every leader's popularity/win-rate " +
                    "stats (enriched with card name/colors/image), every leader-vs-opponent pairing (matchups), " +
                    "each leader's expectedCards and possibleTechs derived from game-weighted decklist usage, " +
                    "copy counts, archetype affinity, and card role; in samples below three decklists, likely core " +
                    "cards are returned separately as observedCards while confident tech choices remain possibleTechs, " +
                    "favoring the leader-specific top win-rate quartile when that cohort is large enough, " +
                    "and the subset of those pairings restricted to the 10 most popular leaders (topMatchups), " +
                    "intended for a lightweight popularity matrix. " +
                    "Data is refreshed by a daily sync job, not computed on request.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Matchups retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MatchupsResponseDto.class)))
    })
    public ResponseEntity<MatchupsResponseDto> getMatchups(WebRequest request) {
        var matchups = matchupsQueryUseCase.getMatchups();
        if (isNotModified(request, matchups.snapshot(), "all")) {
            return null;
        }
        return ResponseEntity.ok()
                .cacheControl(MATCHUPS_CACHE)
                .body(matchupsMapper.toDto(matchups));
    }

    @GetMapping("/overview")
    @Operation(summary = "Get the lightweight matchups overview",
            description = "Returns snapshot metadata, lightweight leader summaries and the top-10 matrix only.")
    public ResponseEntity<MatchupsOverviewResponseDto> getOverview(WebRequest request) {
        var overview = matchupsQueryUseCase.getOverview();
        if (isNotModified(request, overview.snapshot(), "overview")) {
            return null;
        }
        return ResponseEntity.ok()
                .cacheControl(MATCHUPS_CACHE)
                .body(matchupsMapper.toOverviewDto(overview));
    }

    @GetMapping("/leaders/{leaderCode}")
    @Operation(summary = "Get matchups and deck profile for one leader",
            description = "Returns one full leader profile and only the matchup rows viewed from that leader's perspective.")
    public ResponseEntity<LeaderMatchupsResponseDto> getLeaderMatchups(
            @PathVariable String leaderCode,
            WebRequest request
    ) {
        var leaderMatchups = matchupsQueryUseCase.getLeaderMatchups(leaderCode);
        if (leaderMatchups.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var detail = leaderMatchups.orElseThrow();
        if (isNotModified(request, detail.snapshot(), "leader-" + detail.leader().getCardCode())) {
            return null;
        }
        return ResponseEntity.ok()
                .cacheControl(MATCHUPS_CACHE)
                .body(matchupsMapper.toLeaderDto(detail));
    }

    private boolean isNotModified(WebRequest request, MatchupSnapshotInfo snapshot, String representation) {
        if (snapshot == null) {
            return false;
        }
        var etag = String.format("W/\"matchups-%s-%s-%s-%s\"",
                ETAG_VERSION,
                snapshot.getSourceSnapshotId(),
                snapshot.getCardProfileVersion(),
                representation);
        return request.checkNotModified(etag);
    }
}

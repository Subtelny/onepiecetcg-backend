package pl.janda.onepiecetcg.matchups.rest.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.janda.onepiecetcg.matchups.application.port.in.MatchupsQueryUseCase;
import pl.janda.onepiecetcg.matchups.rest.dto.MatchupsResponseDto;
import pl.janda.onepiecetcg.matchups.rest.mapper.MatchupsMapper;

@RestController
@RequestMapping("/api/matchups")
@RequiredArgsConstructor
@Tag(name = "Matchups", description = "Leader popularity and head-to-head matchup statistics")
public class MatchupsController {

    private final MatchupsQueryUseCase matchupsQueryUseCase;

    private final MatchupsMapper matchupsMapper;

    @GetMapping
    @Operation(summary = "Get the current matchups overview",
            description = "Returns the most recently synced snapshot metadata, every leader's popularity/win-rate " +
                    "stats (enriched with card name/colors/image), every leader-vs-opponent pairing (matchups), " +
                    "each leader's expectedCards and possibleTechs derived from game-weighted decklist usage, " +
                    "and the subset of those pairings restricted to the 10 most popular leaders (topMatchups), " +
                    "intended for a lightweight popularity matrix. " +
                    "Data is refreshed by a daily sync job, not computed on request.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Matchups retrieved successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MatchupsResponseDto.class)))
    })
    public ResponseEntity<MatchupsResponseDto> getMatchups() {
        return ResponseEntity.ok(matchupsMapper.toDto(matchupsQueryUseCase.getMatchups()));
    }
}

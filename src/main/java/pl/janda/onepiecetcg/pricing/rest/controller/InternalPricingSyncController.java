package pl.janda.onepiecetcg.pricing.rest.controller;

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
import pl.janda.onepiecetcg.pricing.application.port.in.CardmarketPriceSyncUseCase;
import pl.janda.onepiecetcg.pricing.rest.dto.SyncResultDto;

@RestController
@RequestMapping("/api/internal/sync")
@RequiredArgsConstructor
@Tag(name = "Internal Pricing Sync",
        description = "Dev-only endpoints to manually trigger pricing sync jobs. Requires X-API-Key header.")
@SecurityRequirement(name = "ApiKeyAuth")
public class InternalPricingSyncController {

    private final CardmarketPriceSyncUseCase cardmarketPriceSyncUseCase;

    @PostMapping("/cardmarket-prices")
    @Operation(summary = "Manually sync Cardmarket prices",
            description = "Downloads Cardmarket's public One Piece singles catalog and EUR price guide, resolves "
                    + "stable catalog price references through expansion and local-variant mappings, then appends "
                    + "a historical snapshot. An already stored price-guide publication is reused while missing "
                    + "catalog mappings are repaired without duplicating history.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Sync completed"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid API key")
    })
    public ResponseEntity<SyncResultDto> syncCardmarketPrices() {
        cardmarketPriceSyncUseCase.syncPrices();
        return ResponseEntity.ok(new SyncResultDto(true, "Cardmarket price sync completed"));
    }
}

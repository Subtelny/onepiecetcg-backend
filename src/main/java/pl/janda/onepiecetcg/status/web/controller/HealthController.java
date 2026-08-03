package pl.janda.onepiecetcg.status.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.janda.onepiecetcg.status.web.dto.HealthStatusDto;

@RestController
@Tag(name = "Health", description = "Application liveness endpoints")
public class HealthController {

    @GetMapping({"/", "/health"})
    @Operation(summary = "Check application liveness",
            description = "Returns HTTP 200 when the application process is running and accepting HTTP requests.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Application is running",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = HealthStatusDto.class)))
    })
    public ResponseEntity<HealthStatusDto> health() {
        return ResponseEntity.ok(new HealthStatusDto("UP"));
    }
}

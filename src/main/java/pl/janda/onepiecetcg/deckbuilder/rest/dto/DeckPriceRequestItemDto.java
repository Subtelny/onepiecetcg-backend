package pl.janda.onepiecetcg.deckbuilder.rest.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeckPriceRequestItemDto {

    @NotBlank
    @Size(max = 32)
    private String cardCode;

    @NotBlank
    @Pattern(regexp = "(?i)(?:0|[pr][1-9]\\d*)")
    private String variantIndex;

    @NotNull
    @Min(1)
    @Max(4)
    private Integer quantity;
}

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
public class CreateSharedDeckCardRequest {

    @NotBlank
    @Size(max = 32)
    private String cardNumber;

    @NotNull
    @Min(1)
    @Max(4)
    private Integer quantity;
}

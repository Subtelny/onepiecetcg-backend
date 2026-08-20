package pl.janda.onepiecetcg.deckbuilder.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeckPriceSummaryRequest {

    @NotNull
    @Size(max = 51)
    private List<@NotNull @Valid DeckPriceRequestItemDto> cards;

    @AssertTrue(message = "the total card quantity cannot exceed 51")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isTotalCardCountValid() {
        if (cards == null || cards.stream().anyMatch(card -> card == null || card.getQuantity() == null)) {
            return true;
        }
        return cards.stream().mapToInt(DeckPriceRequestItemDto::getQuantity).sum() <= 51;
    }
}

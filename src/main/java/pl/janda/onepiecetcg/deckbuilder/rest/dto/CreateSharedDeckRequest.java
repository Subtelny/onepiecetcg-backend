package pl.janda.onepiecetcg.deckbuilder.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateSharedDeckRequest {

    @NotBlank
    @Size(max = 80)
    private String name;

    @Size(max = 32)
    private String leaderCardNumber;

    @NotNull
    @Size(max = 50)
    private List<@NotNull @Valid CreateSharedDeckCardRequest> cards;

    @AssertTrue(message = "a shared deck must contain a leader or at least one deck card")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isDeckNotEmpty() {
        if (cards == null) {
            return true;
        }
        return leaderCardNumber != null && !leaderCardNumber.isBlank() || !cards.isEmpty();
    }

    @AssertTrue(message = "the total deck card quantity cannot exceed 50")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isTotalCardCountValid() {
        if (cards == null || cards.stream().anyMatch(card -> card == null || card.getQuantity() == null)) {
            return true;
        }
        return cards.stream().mapToInt(CreateSharedDeckCardRequest::getQuantity).sum() <= 50;
    }

    @AssertTrue(message = "cardNumber values must be unique")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isCardNumbersUnique() {
        if (cards == null || cards.stream().anyMatch(card -> card == null || card.getCardNumber() == null)) {
            return true;
        }
        var cardNumbers = new HashSet<String>();
        return cards.stream()
                .map(CreateSharedDeckCardRequest::getCardNumber)
                .map(String::trim)
                .allMatch(cardNumbers::add);
    }
}

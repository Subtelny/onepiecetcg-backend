package pl.janda.onepiecetcg.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateDeckRequest {
    @NotBlank(message = "Deck name is required")
    private String name;

    private String description;

    @NotNull(message = "Leader card is required")
    private CardDto leader;

    private List<DeckCardDto> cards;

    private String author;
}

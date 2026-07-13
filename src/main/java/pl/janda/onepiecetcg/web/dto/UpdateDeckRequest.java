package pl.janda.onepiecetcg.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateDeckRequest {
    private String name;
    private String description;
    private CardDto leader;
    private List<DeckCardDto> cards;
    private String author;
}

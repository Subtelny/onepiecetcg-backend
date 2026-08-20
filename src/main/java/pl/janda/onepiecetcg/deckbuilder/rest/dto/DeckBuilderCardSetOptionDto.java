package pl.janda.onepiecetcg.deckbuilder.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeckBuilderCardSetOptionDto {
    private String setId;
    private String setName;
    private boolean released;
    private LocalDate releaseDate;
}

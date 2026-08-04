package pl.janda.onepiecetcg.deckbuilder.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeckBuilderCardSetOptionDto {
    private String setId;
    private String setName;
}

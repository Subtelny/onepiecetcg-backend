package pl.janda.onepiecetcg.deckbuilder.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SharedDeckDto {

    private String code;

    private String name;

    private DeckBuilderCardDto leader;

    private List<SharedDeckCardDto> cards;

    private String createdAt;
}

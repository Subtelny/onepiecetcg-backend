package pl.janda.onepiecetcg.cards.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardFaqEntryDto {
    private String question;
    private String answer;
}

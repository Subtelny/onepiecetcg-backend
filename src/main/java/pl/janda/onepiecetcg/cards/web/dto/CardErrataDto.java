package pl.janda.onepiecetcg.cards.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardErrataDto {
    private String cardCode;
    private String cardName;
    private String scopeNote;
    private String beforeText;
    private String afterText;
    private String noticeDate;
    private String sourceUrl;
}

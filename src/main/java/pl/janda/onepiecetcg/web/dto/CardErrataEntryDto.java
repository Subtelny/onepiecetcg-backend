package pl.janda.onepiecetcg.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardErrataEntryDto {
    private String date;
    private String before;
    private String after;
    private String note;
}

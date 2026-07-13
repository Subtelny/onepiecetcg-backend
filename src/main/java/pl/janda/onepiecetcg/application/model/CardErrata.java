package pl.janda.onepiecetcg.application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardErrata {
    private String date;        // YYYY-MM-DD format
    private String before;
    private String after;
    private String note;
}

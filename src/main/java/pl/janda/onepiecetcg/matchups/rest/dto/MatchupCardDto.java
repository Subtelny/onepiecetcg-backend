package pl.janda.onepiecetcg.matchups.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchupCardDto {

    private String cardCode;

    private String name;

    private String imageUrl;

    private String type;

    private Integer cost;

    private Integer power;

    private Integer counter;

    private String effect;

    private BigDecimal inclusionRate;

    private BigDecimal typicalCopies;

    private Integer copies;
}

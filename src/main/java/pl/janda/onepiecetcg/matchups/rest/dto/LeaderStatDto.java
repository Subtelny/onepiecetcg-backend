package pl.janda.onepiecetcg.matchups.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderStatDto {
    private String code;
    private String name;
    private List<String> colors;
    private String imageUrl;
    private BigDecimal popularity;
    private Long matches;
    private BigDecimal winRate;
}

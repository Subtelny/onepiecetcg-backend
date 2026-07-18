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
public class CardSearchResponse {
    private List<CardDto> cards;
    private long totalCount;
    private int page;
    private int limit;
    private boolean hasMore;
}

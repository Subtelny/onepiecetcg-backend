package pl.janda.onepiecetcg.cards.web.dto;

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
    private List<CardSummaryDto> cards;
    private long totalCount;
    private int page;
    private int limit;
    private boolean hasMore;
}

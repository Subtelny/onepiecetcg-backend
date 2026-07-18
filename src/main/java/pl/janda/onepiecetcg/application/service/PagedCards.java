package pl.janda.onepiecetcg.application.service;

import pl.janda.onepiecetcg.application.model.SetCard;

import java.util.List;

public record PagedCards(List<SetCard> cards, long totalCount, int page, int limit) {

    public boolean hasMore() {
        return (long) (page + 1) * limit < totalCount;
    }
}

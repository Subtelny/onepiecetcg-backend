package pl.janda.onepiecetcg.deckbuilder.application.model;

import java.util.List;

public record DeckPriceSummary(List<DeckPriceTotal> totals, int pricedCopies, int totalCopies) {
}

package pl.janda.onepiecetcg.deckbuilder.application.port.in;

import pl.janda.onepiecetcg.deckbuilder.application.model.DeckPriceItem;
import pl.janda.onepiecetcg.deckbuilder.application.model.DeckPriceSummary;

import java.util.List;

public interface DeckPriceUseCase {

    DeckPriceSummary calculateDeckPrice(List<DeckPriceItem> items);
}

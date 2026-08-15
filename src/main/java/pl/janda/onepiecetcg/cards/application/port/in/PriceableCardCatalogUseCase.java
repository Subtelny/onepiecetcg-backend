package pl.janda.onepiecetcg.cards.application.port.in;

import pl.janda.onepiecetcg.cards.application.model.PriceableCard;

import java.util.List;

public interface PriceableCardCatalogUseCase {

    List<PriceableCard> getPriceableCards();
}

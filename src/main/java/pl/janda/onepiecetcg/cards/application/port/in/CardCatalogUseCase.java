package pl.janda.onepiecetcg.cards.application.port.in;

import pl.janda.onepiecetcg.cards.application.model.CardFilterOptions;
import pl.janda.onepiecetcg.cards.application.model.CardSearchQuery;
import pl.janda.onepiecetcg.cards.application.model.PagedCards;
import pl.janda.onepiecetcg.cards.application.model.SetCard;

import java.util.List;

public interface CardCatalogUseCase {

    SetCard getCardById(String id);

    List<SetCard> getVariantsByCardId(String id);

    List<String> getAllCardCodes();

    SetCard getVariantByCardCode(String cardCode, String variant);

    List<SetCard> getRepresentativeCardsByCardCodes(List<String> cardCodes);

    PagedCards searchCards(CardSearchQuery query);

    CardFilterOptions getFilterOptions();
}

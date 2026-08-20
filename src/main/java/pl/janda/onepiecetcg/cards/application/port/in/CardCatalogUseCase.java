package pl.janda.onepiecetcg.cards.application.port.in;

import pl.janda.onepiecetcg.cards.application.model.*;

import java.util.List;

public interface CardCatalogUseCase {

    SetCard getCardById(String id);

    List<SetCard> getVariantsByCardId(String id);

    List<String> getAllCardCodes();

    SetCard getVariantByCardCode(String cardCode, String variant);

    List<SetCard> getCardsByVariantReferences(List<CardVariantReference> references);

    List<SetCard> getRepresentativeCardsByCardCodes(List<String> cardCodes);

    PagedCards searchCards(CardSearchQuery query);

    CardFilterOptions getFilterOptions();
}

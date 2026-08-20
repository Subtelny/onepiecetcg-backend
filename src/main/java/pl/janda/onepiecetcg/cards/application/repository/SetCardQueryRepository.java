package pl.janda.onepiecetcg.cards.application.repository;

import pl.janda.onepiecetcg.cards.application.model.CardSearchCriteria;
import pl.janda.onepiecetcg.cards.application.model.CardSummary;
import pl.janda.onepiecetcg.cards.application.model.PriceableCard;
import pl.janda.onepiecetcg.cards.application.model.SetCard;

import java.util.List;
import java.util.Optional;

public interface SetCardQueryRepository {

    Optional<SetCard> findById(Long id);

    List<SetCard> findByCardSetId(String cardSetId);

    List<SetCard> findByCardSetIdIn(List<String> cardSetIds);

    Optional<SetCard> findByCardSetIdAndVariantIndex(String cardSetId, String variantIndex);

    List<SetCard> findRepresentativesByCardSetIds(List<String> cardSetIds);

    List<String> findAllCardCodes();

    List<PriceableCard> findAllPriceableCards();

    List<CardSummary> search(CardSearchCriteria criteria);

    long countSearch(CardSearchCriteria criteria);
}

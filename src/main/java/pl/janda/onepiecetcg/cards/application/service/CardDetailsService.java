package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.cards.application.model.CardDetails;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.port.in.CardCatalogUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.CardDetailsUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.CardErrataQueryUseCase;
import pl.janda.onepiecetcg.cards.application.port.in.CardFaqQueryUseCase;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardDetailsService implements CardDetailsUseCase {

    private final CardCatalogUseCase cardCatalogUseCase;

    private final CardErrataQueryUseCase cardErrataQueryUseCase;

    private final CardFaqQueryUseCase cardFaqQueryUseCase;

    @Override
    public CardDetails getCardById(String id) {
        return resolve(cardCatalogUseCase.getCardById(id));
    }

    @Override
    public CardDetails getCardByCode(String cardCode, Integer variant) {
        return resolve(cardCatalogUseCase.getVariantByCardCode(cardCode, variant));
    }

    @Override
    public List<CardDetails> getCardVariants(String id) {
        var cards = cardCatalogUseCase.getVariantsByCardId(id);
        var cardCodes = cards.stream().map(SetCard::getCardSetId).toList();
        var errataByCode = cardErrataQueryUseCase.historyByCardCodes(cardCodes);
        var faqByCode = cardFaqQueryUseCase.historyByCardCodes(cardCodes);

        return cards.stream()
                .map(card -> new CardDetails(
                        card,
                        errataByCode.getOrDefault(card.getCardSetId(), List.of()),
                        faqByCode.getOrDefault(card.getCardSetId(), List.of())))
                .toList();
    }

    private CardDetails resolve(SetCard card) {
        var cardCode = card.getCardSetId();
        if (cardCode == null) {
            return new CardDetails(card, List.of(), List.of());
        }
        var errata = cardErrataQueryUseCase.historyByCardCodes(List.of(cardCode))
                .getOrDefault(cardCode, List.of());
        var faq = cardFaqQueryUseCase.historyByCardCodes(List.of(cardCode))
                .getOrDefault(cardCode, List.of());
        return new CardDetails(card, errata, faq);
    }
}

package pl.janda.onepiecetcg.cards.application.port.in;

import pl.janda.onepiecetcg.cards.application.model.CardFaq;

import java.util.List;
import java.util.Map;

public interface CardFaqQueryUseCase {

    Map<String, List<CardFaq>> historyByCardCodes(List<String> cardCodes);
}

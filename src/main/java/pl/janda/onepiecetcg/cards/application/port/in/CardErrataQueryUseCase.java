package pl.janda.onepiecetcg.cards.application.port.in;

import pl.janda.onepiecetcg.cards.application.model.CardErrata;

import java.util.List;
import java.util.Map;

public interface CardErrataQueryUseCase {

    List<CardErrata> listAll();

    Map<String, List<CardErrata>> historyByCardCodes(List<String> cardCodes);
}

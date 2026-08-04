package pl.janda.onepiecetcg.cards.application.port.in;

import pl.janda.onepiecetcg.cards.application.model.CardDetails;

import java.util.List;

public interface CardDetailsUseCase {

    CardDetails getCardById(String id);

    CardDetails getCardByCode(String cardCode, Integer variant);

    List<CardDetails> getCardVariants(String id);
}

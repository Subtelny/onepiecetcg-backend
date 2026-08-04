package pl.janda.onepiecetcg.deckbuilder.application.model;

import pl.janda.onepiecetcg.cards.application.model.SetCard;

import java.util.List;

public record SharedDeckDetails(
        SharedDeck deck,
        SetCard leader,
        List<SharedDeckCardDetails> cards
) {
}

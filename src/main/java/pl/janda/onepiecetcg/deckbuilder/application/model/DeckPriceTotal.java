package pl.janda.onepiecetcg.deckbuilder.application.model;

import java.math.BigDecimal;

public record DeckPriceTotal(String currency, BigDecimal amount) {
}

package pl.janda.onepiecetcg.deckbuilder.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.cards.application.model.CardVariantReference;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.port.in.CardCatalogUseCase;
import pl.janda.onepiecetcg.deckbuilder.application.model.DeckPriceItem;
import pl.janda.onepiecetcg.deckbuilder.application.model.DeckPriceSummary;
import pl.janda.onepiecetcg.deckbuilder.application.model.DeckPriceTotal;
import pl.janda.onepiecetcg.deckbuilder.application.port.in.DeckPriceUseCase;
import pl.janda.onepiecetcg.pricing.application.model.PriceQuote;
import pl.janda.onepiecetcg.pricing.application.model.PriceSource;
import pl.janda.onepiecetcg.pricing.application.port.in.PriceQueryUseCase;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DeckPriceService implements DeckPriceUseCase {

    private final CardCatalogUseCase cardCatalogUseCase;

    private final PriceQueryUseCase priceQueryUseCase;

    @Override
    public DeckPriceSummary calculateDeckPrice(List<DeckPriceItem> items) {
        if (items == null || items.isEmpty()) {
            return new DeckPriceSummary(List.of(), 0, 0);
        }

        var quantitiesByReference = new LinkedHashMap<CardVariantReference, Integer>();
        for (var item : items) {
            var reference = new CardVariantReference(
                    item.cardCode().trim(),
                    item.variantIndex().trim().toLowerCase(Locale.ROOT));
            quantitiesByReference.merge(reference, item.quantity(), Integer::sum);
        }

        var cardsByReference = cardCatalogUseCase
                .getCardsByVariantReferences(List.copyOf(quantitiesByReference.keySet()))
                .stream()
                .collect(Collectors.toMap(
                        card -> new CardVariantReference(card.getCardSetId(), card.getVariantIndex()),
                        Function.identity(),
                        (first, second) -> first,
                        LinkedHashMap::new));
        var pricesByReference = priceQueryUseCase.getLatestPricesByReferences(cardsByReference.values().stream()
                .map(SetCard::getPriceReference)
                .toList());

        var totals = new LinkedHashMap<String, BigDecimal>();
        var pricedCopies = 0;
        var totalCopies = quantitiesByReference.values().stream().mapToInt(Integer::intValue).sum();
        for (var entry : quantitiesByReference.entrySet()) {
            var card = cardsByReference.get(entry.getKey());
            if (card == null || card.getPriceReference() == null) {
                continue;
            }
            var quote = selectCurrentQuote(pricesByReference.get(card.getPriceReference()));
            if (quote == null) {
                continue;
            }
            var amount = currentPrice(quote);
            var currency = quote.getCurrency() == null || quote.getCurrency().isBlank()
                    ? "EUR"
                    : quote.getCurrency();
            totals.merge(currency, amount.multiply(BigDecimal.valueOf(entry.getValue())), BigDecimal::add);
            pricedCopies += entry.getValue();
        }

        var priceTotals = totals.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new DeckPriceTotal(entry.getKey(), entry.getValue()))
                .toList();
        return new DeckPriceSummary(priceTotals, pricedCopies, totalCopies);
    }

    private PriceQuote selectCurrentQuote(List<PriceQuote> quotes) {
        if (quotes == null) {
            return null;
        }
        return quotes.stream()
                .filter(quote -> currentPrice(quote) != null)
                .max(Comparator
                        .comparingInt(this::sourcePriority)
                        .thenComparingInt(this::priceCompleteness))
                .orElse(null);
    }

    private int sourcePriority(PriceQuote quote) {
        return quote.getSource() == PriceSource.CARDMARKET ? 1 : 0;
    }

    private int priceCompleteness(PriceQuote quote) {
        var count = 0;
        if (isPositive(quote.getTrendPrice())) {
            count++;
        }
        if (isPositive(quote.getAveragePrice())) {
            count++;
        }
        if (isPositive(quote.getLowPrice())) {
            count++;
        }
        return count;
    }

    private BigDecimal currentPrice(PriceQuote quote) {
        if (quote == null) {
            return null;
        }
        if (isPositive(quote.getTrendPrice())) {
            return quote.getTrendPrice();
        }
        return isPositive(quote.getAveragePrice()) ? quote.getAveragePrice() : null;
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.signum() > 0;
    }
}

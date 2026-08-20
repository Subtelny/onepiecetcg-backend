package pl.janda.onepiecetcg.deckbuilder.rest.mapper;

import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.deckbuilder.application.model.DeckPriceItem;
import pl.janda.onepiecetcg.deckbuilder.application.model.DeckPriceSummary;
import pl.janda.onepiecetcg.deckbuilder.rest.dto.DeckPriceSummaryDto;
import pl.janda.onepiecetcg.deckbuilder.rest.dto.DeckPriceSummaryRequest;
import pl.janda.onepiecetcg.deckbuilder.rest.dto.DeckPriceTotalDto;

import java.util.List;

@Component
public class DeckPriceMapper {

    public List<DeckPriceItem> toItems(DeckPriceSummaryRequest request) {
        return request.getCards().stream()
                .map(card -> new DeckPriceItem(
                        card.getCardCode(),
                        card.getVariantIndex(),
                        card.getQuantity()))
                .toList();
    }

    public DeckPriceSummaryDto toDto(DeckPriceSummary summary) {
        return DeckPriceSummaryDto.builder()
                .totals(summary.totals().stream()
                        .map(total -> DeckPriceTotalDto.builder()
                                .currency(total.currency())
                                .amount(total.amount())
                                .build())
                        .toList())
                .pricedCopies(summary.pricedCopies())
                .totalCopies(summary.totalCopies())
                .build();
    }
}

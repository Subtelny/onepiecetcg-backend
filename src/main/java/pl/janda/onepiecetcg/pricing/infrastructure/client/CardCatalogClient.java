package pl.janda.onepiecetcg.pricing.infrastructure.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.cards.application.port.in.PriceableCardCatalogUseCase;
import pl.janda.onepiecetcg.pricing.application.client.PriceableSingleCatalogClient;
import pl.janda.onepiecetcg.pricing.application.model.PriceableSingle;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CardCatalogClient implements PriceableSingleCatalogClient {

    private final PriceableCardCatalogUseCase priceableCardCatalogUseCase;

    @Override
    public List<PriceableSingle> fetchPriceableSingles() {
        return priceableCardCatalogUseCase.getPriceableCards().stream()
                .map(card -> PriceableSingle.builder()
                        .priceReference(card.getPriceReference())
                        .sourceCardId(card.getSourceCardId())
                        .cardCode(card.getCardCode())
                        .releaseId(card.getReleaseId())
                        .releaseName(card.getReleaseName())
                        .setName(card.getSetName())
                        .variantIndex(card.getVariantIndex())
                        .build())
                .toList();
    }
}

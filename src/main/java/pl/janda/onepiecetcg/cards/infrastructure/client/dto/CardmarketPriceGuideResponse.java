package pl.janda.onepiecetcg.cards.infrastructure.client.dto;

import java.util.List;

public record CardmarketPriceGuideResponse(
        String version,
        String createdAt,
        List<CardmarketPriceResponse> priceGuides
) {
}

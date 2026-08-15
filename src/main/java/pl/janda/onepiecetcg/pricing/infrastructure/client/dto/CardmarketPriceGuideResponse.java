package pl.janda.onepiecetcg.pricing.infrastructure.client.dto;

import java.util.List;

public record CardmarketPriceGuideResponse(
        String version,
        String createdAt,
        List<CardmarketPriceResponse> priceGuides
) {
}

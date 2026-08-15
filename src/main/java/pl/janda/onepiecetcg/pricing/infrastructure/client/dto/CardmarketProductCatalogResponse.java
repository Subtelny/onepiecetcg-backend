package pl.janda.onepiecetcg.pricing.infrastructure.client.dto;

import java.util.List;

public record CardmarketProductCatalogResponse(
        String version,
        String createdAt,
        List<CardmarketProductResponse> products
) {
}

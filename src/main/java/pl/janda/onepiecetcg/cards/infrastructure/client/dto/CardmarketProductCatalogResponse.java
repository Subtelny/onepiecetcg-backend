package pl.janda.onepiecetcg.cards.infrastructure.client.dto;

import java.util.List;

public record CardmarketProductCatalogResponse(
        String version,
        String createdAt,
        List<CardmarketProductResponse> products
) {
}

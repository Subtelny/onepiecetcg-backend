package pl.janda.onepiecetcg.pricing.infrastructure.client.dto;

public record CardmarketProductResponse(
        Long idProduct,
        Long idExpansion,
        Long idMetacard,
        String name,
        String dateAdded
) {
}

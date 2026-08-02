package pl.janda.onepiecetcg.cards.infrastructure.client.dto;

public record CardmarketProductResponse(
        Long idProduct,
        Long idExpansion,
        Long idMetacard,
        String name,
        String dateAdded
) {
}

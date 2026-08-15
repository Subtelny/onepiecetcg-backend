package pl.janda.onepiecetcg.pricing.infrastructure.client;

import org.junit.jupiter.api.Test;
import pl.janda.onepiecetcg.pricing.application.model.CardmarketProductPageRequest;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class CardmarketProductPageClientTest {

    @Test
    void parseProductPage_extractsExpansionAndVersionFromCanonicalUrl() {
        var request = CardmarketProductPageRequest.builder()
                .productId(767954L)
                .expansionId(5585L)
                .build();

        var result = CardmarketProductPageClient.parseProductPage(
                request,
                URI.create("https://www.cardmarket.com/en/OnePiece/Products/Singles/"
                        + "Memorial-Collection/Kouzuki-Oden-EB01-001-V2"));

        assertThat(result).hasValueSatisfying(page -> {
            assertThat(page.getProductId()).isEqualTo(767954L);
            assertThat(page.getExpansionSlug()).isEqualTo("Memorial-Collection");
            assertThat(page.getVersion()).isEqualTo(2);
        });
    }
}

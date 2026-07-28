package pl.janda.onepiecetcg.cards.infrastructure.client;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import pl.janda.onepiecetcg.cards.application.model.CardFaqListingEntry;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

class OnePieceCardGameFaqClientTest {

    private static final String FAQ_LISTING_HTML = """
            <html><body>
              <ul class="faqList">
                <li>
                  <a href="/pdf/qa_rules.pdf?20250228">
                    <span class="date">February 28, 2025 updated</span>
                    <h3 class="title">General Rules</h3>
                  </a>
                </li>
                <li>
                  <a href="/pdf/qa_op01.pdf?20240405">
                    <span class="date">April 5, 2024 updated</span>
                    <h3 class="title">ROMANCE DAWN [OP-01]</h3>
                  </a>
                </li>
                <li>
                  <a href="/pdf/qa_st-36.pdf?20260717">
                    <span class="date">July 17, 2026 updated NEW</span>
                    <h3 class="title">STARTER DECK -YELLOW Eustass&quot;Captain&quot;Kid- [ST-36]</h3>
                  </a>
                </li>
              </ul>
            </body></html>
            """;

    private final OnePieceCardGameFaqClient client = new OnePieceCardGameFaqClient(
            "https://en.onepiece-cardgame.com/rules/faq/", RestClient.builder());

    @Test
    void parseListingExcludesGeneralRulesAndParsesSlugAndDateFromPdfLink() {
        var document = Jsoup.parse(FAQ_LISTING_HTML, "https://en.onepiece-cardgame.com/rules/faq/");

        var entries = client.parseListing(document);

        assertThat(entries).extracting(CardFaqListingEntry::setId).containsExactly("op01", "st-36");
        assertThat(entries).extracting(CardFaqListingEntry::publishedDate)
                .containsExactly(LocalDate.of(2024, 4, 5), LocalDate.of(2026, 7, 17));
        assertThat(entries).extracting(CardFaqListingEntry::pdfUrl)
                .containsExactly(
                        "https://en.onepiece-cardgame.com/pdf/qa_op01.pdf?20240405",
                        "https://en.onepiece-cardgame.com/pdf/qa_st-36.pdf?20260717");
    }

    @Test
    void parsePdfExtractsColumnAwareRowsFromSampleFixturePdf() throws IOException {
        var pdfBytes = readFixture("qa_st-36_faq_sample.pdf");
        var publishedDate = LocalDate.of(2026, 7, 17);
        var sourceUrl = "https://en.onepiece-cardgame.com/pdf/qa_st-36.pdf?20260717";

        var rows = client.parsePdf(pdfBytes, "st-36", publishedDate, sourceUrl);

        assertThat(rows).hasSize(2);

        var first = rows.get(0);
        assertThat(first.getCardCode()).isEqualTo("ST36-001");
        assertThat(first.getCardName()).isEqualTo("Cavendish");
        assertThat(first.getQuestion()).isEqualTo("Does this effect trigger during the opponent's turn?");
        assertThat(first.getAnswer()).isEqualTo("Yes, it does.");
        assertThat(first.getSetId()).isEqualTo("st-36");
        assertThat(first.getPublishedDate()).isEqualTo(publishedDate);
        assertThat(first.getSourceUrl()).isEqualTo(sourceUrl);

        var second = rows.get(1);
        assertThat(second.getCardCode()).isEqualTo("ST36-002");
        assertThat(second.getCardName()).isEqualTo("Killer");
        assertThat(second.getQuestion()).isEqualTo("Can this be used twice?");
        assertThat(second.getAnswer()).isEqualTo("No, only once.");
    }

    private byte[] readFixture(String name) throws IOException {
        try (var in = getClass().getClassLoader().getResourceAsStream(name)) {
            return Objects.requireNonNull(in, "Missing test fixture: " + name).readAllBytes();
        }
    }
}

package pl.janda.onepiecetcg.infrastructure.client;

import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import pl.janda.onepiecetcg.application.model.CardErrata;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OnePieceCardGameErrataClientTest {

    private static final String ERRATA_URL = "https://en.onepiece-cardgame.com/rules/errata_card/";
    private static final String TOPICS_URL = "https://en.onepiece-cardgame.com/topics/";

    // Covers both recurring HTML shapes on the real page:
    // - Pattern A (recent entries): section.contentsLCol > h4.mediumTit (date) + div.detailCol > h5.smallTitRed + dl
    // - Pattern B (older bulk entries): div.errataModal > h5.smallTitRed (date embedded via <br>) + dl
    // Plus two entries that must be skipped: an image-only fix (no Before/After) and an unparseable heading.
    private static final String SAMPLE_HTML = """
            <html>
            <body>
              <section class="contentsLCol">
                <h4 class="mediumTit">March 3, 2024</h4>
                <div class="detailCol" id="errata_1">
                  <h5 class="smallTitRed">OP13-119 Charlotte Katakuri</h5>
                  <ul class="commonNoticeList">
                    <li>*Also applies to parallel card version.</li>
                  </ul>
                  <dl>
                    <dt>Before:</dt>
                    <dd>[Trigger] KO 1 of your Leader or Character cards.</dd>
                    <dt>After:</dt>
                    <dd>[Trigger] KO up to 1 of your Leader or Character cards.</dd>
                  </dl>
                </div>
                <div class="detailCol" id="errata_2">
                  <h5 class="smallTitRed">OP14-009 Roronoa Zoro</h5>
                  <dl>
                    <dt>Note:</dt>
                    <dd>Corrected the card image only.</dd>
                  </dl>
                </div>
                <div class="detailCol" id="errata_3">
                  <h5 class="smallTitRed">Some heading without a card code</h5>
                  <dl>
                    <dt>Before:</dt>
                    <dd>foo</dd>
                    <dt>After:</dt>
                    <dd>bar</dd>
                  </dl>
                </div>
              </section>
              <section class="cardPackCol">
                <ul class="errataPopupCol">
                  <li>
                    <div class="errataModal" id="errata_old_1">
                      <h5 class="smallTitRed">February 17, 2023<br>OP01-002 Trafalgar Law (Parallel)</h5>
                      <dl>
                        <dt>Before:</dt>
                        <dd>[When Attacking] Draw 1 card.</dd>
                        <dt>After:</dt>
                        <dd>[When Attacking] You may draw 1 card.</dd>
                      </dl>
                    </div>
                  </li>
                </ul>
              </section>
            </body>
            </html>
            """;

    // Topics feed sample: 2 genuine standalone errata notices (one underscore href, one hyphenated
    // href - mirroring the real op14-009 site quirk), 1 redundant entry linking back to the rules
    // page (must be excluded), and 1 unrelated topic (must be excluded).
    private static final String TOPICS_LIST_HTML = """
            <html>
            <body>
              <ul>
                <li class="topicDetail">
                  <a href="/topics/notice_op15-023.php">
                    <dl>
                      <dt class="topicTit"><span class="js_topicTit">Apology for the errata and revision in card effect text has been announced.</span></dt>
                      <dd class="topicDate">March 13, 2026</dd>
                    </dl>
                  </a>
                </li>
                <li class="topicDetail">
                  <a href="/topics/notice-op14-009.php">
                    <dl>
                      <dt class="topicTit"><span class="js_topicTit">Apology for the errata and revision in card effect text has been announced.</span></dt>
                      <dd class="topicDate">December 19, 2025</dd>
                    </dl>
                  </a>
                </li>
                <li class="topicDetail">
                  <a href="/rules/errata_card/">
                    <dl>
                      <dt class="topicTit"><span class="js_topicTit">Errata Cards [OP-01] has been updated.</span></dt>
                      <dd class="topicDate">January 1, 2025</dd>
                    </dl>
                  </a>
                </li>
                <li class="topicDetail">
                  <a href="/topics/notice_unrelated.php">
                    <dl>
                      <dt class="topicTit"><span class="js_topicTit">New booster pack has been announced.</span></dt>
                      <dd class="topicDate">January 2, 2025</dd>
                    </dl>
                  </a>
                </li>
              </ul>
            </body>
            </html>
            """;

    // Real-shaped topic notice detail page (based on notice_op13-119.php), including a
    // deliberately mismatched div id vs p.cardNum, mirroring the real op15-023 site bug, to prove
    // the parser never relies on the id.
    private static final String TOPIC_DETAIL_HTML = """
            <html>
            <body>
              <main>
                <h3 class="pageTit">Apology for the errata and revision in card effect text of BOOSTER PACK -CARRYING ON HIS WILL- [OP-13]</h3>
                <div class="pageTitInfoCol"><span class="pageTitDate">October 30, 2025</span></div>
                <div class="contentsWrap">
                  <div class="contentsLCol">
                    <div id="OP99-999" class="detailCol mtM">
                      <p class="cardNum">OP13-119</p>
                      <h6 class="cardName">Portgas.D.Ace<br>(Wanted Poster Design)</h6>
                      <dl class="impNoticeBox mtM isWhite isBlue">
                        <dt>Before</dt>
                        <dd><p>[On Play] If your Leader is multicolored, set up to 4 of your DON!! cards as active.</p></dd>
                      </dl>
                      <dl class="impNoticeBox mtS isWhite">
                        <dt>After</dt>
                        <dd><p>[On Play] Give up to 1 rested DON!! card to your Leader.</p></dd>
                      </dl>
                    </div>
                    <ul class="commonNoticeList isHalf mtS">
                      <li>*Only the Wanted Poster Version of this card is affected.</li>
                    </ul>
                  </div>
                </div>
              </main>
            </body>
            </html>
            """;

    private final OnePieceCardGameErrataClient client = new OnePieceCardGameErrataClient(ERRATA_URL, TOPICS_URL);

    @Test
    void parse_extractsPatternAEntry_withDateFromPrecedingHeadingAndScopeNoteFromNoticeList() {
        var document = Jsoup.parse(SAMPLE_HTML);

        var result = client.parse(document);

        var katakuri = result.stream().filter(e -> e.getCardCode().equals("OP13-119")).findFirst().orElseThrow();
        assertThat(katakuri.getCardName()).isEqualTo("Charlotte Katakuri");
        assertThat(katakuri.getScopeNote()).isEqualTo("Also applies to parallel card version.");
        assertThat(katakuri.getBeforeText()).isEqualTo("[Trigger] KO 1 of your Leader or Character cards.");
        assertThat(katakuri.getAfterText()).isEqualTo("[Trigger] KO up to 1 of your Leader or Character cards.");
        assertThat(katakuri.getNoticeDate()).isEqualTo(LocalDate.of(2024, 3, 3));
        assertThat(katakuri.getSourceUrl()).isEqualTo(ERRATA_URL + "#errata_1");
    }

    @Test
    void parse_extractsPatternBEntry_withDateEmbeddedInHeadingAndScopeNoteFromParenthetical() {
        var document = Jsoup.parse(SAMPLE_HTML);

        var result = client.parse(document);

        var law = result.stream().filter(e -> e.getCardCode().equals("OP01-002")).findFirst().orElseThrow();
        assertThat(law.getCardName()).isEqualTo("Trafalgar Law");
        assertThat(law.getScopeNote()).isEqualTo("Parallel");
        assertThat(law.getBeforeText()).isEqualTo("[When Attacking] Draw 1 card.");
        assertThat(law.getAfterText()).isEqualTo("[When Attacking] You may draw 1 card.");
        assertThat(law.getNoticeDate()).isEqualTo(LocalDate.of(2023, 2, 17));
        assertThat(law.getSourceUrl()).isEqualTo(ERRATA_URL + "#errata_old_1");
    }

    @Test
    void parse_skipsImageOnlyFixWithNoBeforeAfterText_andUnparseableHeading() {
        var document = Jsoup.parse(SAMPLE_HTML);

        var result = client.parse(document);

        assertThat(result).hasSize(2);
        assertThat(result).noneMatch(e -> "OP14-009".equals(e.getCardCode()));
    }

    @Test
    void selectErrataTopicLinks_returnsOnlyGenuineStandaloneNoticePages() {
        var document = Jsoup.parse(TOPICS_LIST_HTML, TOPICS_URL);

        var links = client.selectErrataTopicLinks(document);

        assertThat(links).containsExactlyInAnyOrder(
                "https://en.onepiece-cardgame.com/topics/notice_op15-023.php",
                "https://en.onepiece-cardgame.com/topics/notice-op14-009.php"
        );
    }

    @Test
    void parseTopicDetailPage_extractsCardCodeFromCardNumElement_notFromMismatchedDivId() {
        var document = Jsoup.parse(TOPIC_DETAIL_HTML, "https://en.onepiece-cardgame.com/topics/notice_op13-119.php");

        var result = client.parseTopicDetailPage(document, "https://en.onepiece-cardgame.com/topics/notice_op13-119.php");

        assertThat(result).hasSize(1);
        var ace = result.get(0);
        assertThat(ace.getCardCode()).isEqualTo("OP13-119");
        assertThat(ace.getCardName()).isEqualTo("Portgas.D.Ace (Wanted Poster Design)");
        assertThat(ace.getBeforeText()).isEqualTo("[On Play] If your Leader is multicolored, set up to 4 of your DON!! cards as active.");
        assertThat(ace.getAfterText()).isEqualTo("[On Play] Give up to 1 rested DON!! card to your Leader.");
        assertThat(ace.getScopeNote()).isEqualTo("Only the Wanted Poster Version of this card is affected.");
        assertThat(ace.getNoticeDate()).isEqualTo(LocalDate.of(2025, 10, 30));
        assertThat(ace.getSourceUrl()).isEqualTo("https://en.onepiece-cardgame.com/topics/notice_op13-119.php");
    }

    @Test
    void mergeDedup_keepsAllPrimaryEntries_andOnlyNewSecondaryEntriesByCardCodeAndDate() {
        var duplicate = CardErrata.builder().cardCode("OP01-001").noticeDate(LocalDate.of(2023, 1, 1)).build();
        var primary = List.of(duplicate);
        var secondaryDuplicate = CardErrata.builder().cardCode("OP01-001").noticeDate(LocalDate.of(2023, 1, 1)).build();
        var secondaryNew = CardErrata.builder().cardCode("OP13-119").noticeDate(LocalDate.of(2025, 10, 30)).build();
        var secondary = List.of(secondaryDuplicate, secondaryNew);

        var result = client.mergeDedup(primary, secondary);

        assertThat(result).hasSize(2);
        assertThat(result).containsExactly(duplicate, secondaryNew);
    }
}

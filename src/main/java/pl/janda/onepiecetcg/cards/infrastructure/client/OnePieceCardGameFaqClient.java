package pl.janda.onepiecetcg.cards.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import pl.janda.onepiecetcg.cards.application.client.CardFaqApiClient;
import pl.janda.onepiecetcg.cards.application.model.CardFaq;
import pl.janda.onepiecetcg.cards.application.model.CardFaqListingEntry;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;


@Slf4j
@Component
public class OnePieceCardGameFaqClient implements CardFaqApiClient {

    private static final Pattern PDF_LINK = Pattern.compile("/pdf/qa_([a-z0-9-]+)\\.pdf\\?(\\d{8})", Pattern.CASE_INSENSITIVE);
    private static final DateTimeFormatter LINK_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Pattern CARD_CODE = Pattern.compile("^[A-Za-z0-9]+-\\d+$");
    private static final String GENERAL_RULES_SLUG = "rules";


    private static final float CARD_NO_MAX_X = 60f;
    private static final float CARD_NAME_MAX_X = 150f;
    private static final float QUESTION_MAX_X = 300f;

    private final String faqUrl;
    private final RestClient restClient;

    public OnePieceCardGameFaqClient(@Value("${onepiece-cardgame.faq-url}") String faqUrl,
                                       RestClient.Builder restClientBuilder) {
        this.faqUrl = faqUrl;
        this.restClient = restClientBuilder.build();
    }

    @Override
    public List<CardFaqListingEntry> fetchFaqListing() {
        try {
            var document = Jsoup.connect(faqUrl).get();
            return parseListing(document);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to fetch card FAQ listing from " + faqUrl, e);
        }
    }

    List<CardFaqListingEntry> parseListing(Document document) {
        var seenUrls = new HashSet<String>();
        var entries = new ArrayList<CardFaqListingEntry>();
        for (var link : document.select("a[href]")) {
            var href = link.absUrl("href");
            var matcher = PDF_LINK.matcher(href);
            if (!matcher.find() || !seenUrls.add(href)) {
                continue;
            }
            var slug = matcher.group(1).toLowerCase(Locale.ENGLISH);
            if (GENERAL_RULES_SLUG.equals(slug)) {
                continue;
            }
            var publishedDate = LocalDate.parse(matcher.group(2), LINK_DATE_FORMAT);
            entries.add(new CardFaqListingEntry(slug, publishedDate, href));
        }
        return entries;
    }

    @Override
    public List<CardFaq> fetchFaqEntries(String setId, LocalDate publishedDate, String pdfUrl) {
        var pdfBytes = restClient.get().uri(pdfUrl).retrieve().body(byte[].class);
        if (pdfBytes == null) {
            log.warn("Empty PDF response from {}, skipping FAQ set {}", pdfUrl, setId);
            return List.of();
        }
        return parsePdf(pdfBytes, setId, publishedDate, pdfUrl);
    }

    List<CardFaq> parsePdf(byte[] pdfBytes, String setId, LocalDate publishedDate, String pdfUrl) {
        var rows = new ArrayList<CardFaq>();
        try (var document = Loader.loadPDF(pdfBytes)) {
            var stripper = new RowExtractingStripper(rows, setId, publishedDate, pdfUrl);
            stripper.getText(document);
            stripper.flushCurrentRow();
        } catch (IOException e) {
            log.warn("Failed to parse FAQ PDF {} for set {}, skipping", pdfUrl, setId, e);
            return List.of();
        }
        return rows;
    }


    private static final class RowExtractingStripper extends PDFTextStripper {

        private final List<CardFaq> results;
        private final String setId;
        private final LocalDate publishedDate;
        private final String sourceUrl;

        private String currentCardCode;
        private String currentCardName;
        private final StringBuilder currentQuestion = new StringBuilder();
        private final StringBuilder currentAnswer = new StringBuilder();

        private RowExtractingStripper(List<CardFaq> results, String setId, LocalDate publishedDate, String sourceUrl) throws IOException {
            this.results = results;
            this.setId = setId;
            this.publishedDate = publishedDate;
            this.sourceUrl = sourceUrl;
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) {
            if (textPositions.isEmpty()) {
                return;
            }
            var x = textPositions.getFirst().getX();
            var trimmed = normalizeTypography(text.trim());
            if (trimmed.isEmpty()) {
                return;
            }

            if (x < CARD_NO_MAX_X) {
                if (CARD_CODE.matcher(trimmed).matches()) {
                    flushCurrentRow();
                    currentCardCode = trimmed;
                }

            } else if (x < CARD_NAME_MAX_X) {
                currentCardName = currentCardName == null ? trimmed : currentCardName + " " + trimmed;
            } else if (x < QUESTION_MAX_X) {
                append(currentQuestion, trimmed);
            } else {
                append(currentAnswer, trimmed);
            }
        }

        private void append(StringBuilder builder, String text) {
            if (!builder.isEmpty()) {
                builder.append(' ');
            }
            builder.append(text);
        }

        private String normalizeTypography(String text) {
            return text
                    .replace('\u2018', '\'')
                    .replace('\u2019', '\'');
        }

        private void flushCurrentRow() {
            if (currentCardCode != null && !currentQuestion.isEmpty() && !currentAnswer.isEmpty()) {
                results.add(CardFaq.builder()
                        .cardCode(currentCardCode)
                        .cardName(currentCardName)
                        .setId(setId)
                        .question(currentQuestion.toString())
                        .answer(currentAnswer.toString())
                        .publishedDate(publishedDate)
                        .sourceUrl(sourceUrl)
                        .build());
            }
            currentCardCode = null;
            currentCardName = null;
            currentQuestion.setLength(0);
            currentAnswer.setLength(0);
        }
    }
}

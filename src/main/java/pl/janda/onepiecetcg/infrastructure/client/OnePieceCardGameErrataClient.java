package pl.janda.onepiecetcg.infrastructure.client;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.application.client.CardErrataApiClient;
import pl.janda.onepiecetcg.application.model.CardErrata;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Scrapes card errata from en.onepiece-cardgame.com (no JSON API exists for this data, unlike
 * optcgapi.com used by the other sync jobs). Combines two independent HTML sources on the same site:
 * <ul>
 *   <li>The canonical rules page ({@code /rules/errata_card/}), which mixes two recurring HTML
 *   structures anchored around an {@code h5.smallTitRed} heading paired with the nearest {@code dl}
 *   of Before/After text - see {@link #parse(Document)}.</li>
 *   <li>Individual "Apology for the errata..." notice pages under {@code /topics/notice_*.php},
 *   which are NOT all listed on the rules page, discovered by scanning the {@code /topics/} feed
 *   for entries mentioning "errata" - see {@link #parseTopicDetailPage(Document, String)}.</li>
 * </ul>
 * Results from both sources are combined and deduplicated by (cardCode, noticeDate) - see
 * CardErrata's sync job for how the combined results are used.
 */
@Slf4j
@Component
public class OnePieceCardGameErrataClient implements CardErrataApiClient {

    private static final Pattern CODE_AND_NAME = Pattern.compile("^([A-Za-z]+\\d+-\\d+)\\s+(.*)$");
    private static final Pattern TRAILING_PARENTHETICAL = Pattern.compile("^(.*?)\\s*\\(([^)]+)\\)\\s*$");
    private static final Pattern NOTE_PREFIX = Pattern.compile("(?i)^(?:\\*\\s*|note:\\s*)");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH);

    private final String errataUrl;
    private final String topicsUrl;

    public OnePieceCardGameErrataClient(@Value("${onepiece-cardgame.errata-url}") String errataUrl,
                                         @Value("${onepiece-cardgame.topics-url}") String topicsUrl) {
        this.errataUrl = errataUrl;
        this.topicsUrl = topicsUrl;
    }

    @Override
    public List<CardErrata> fetchAllErrata() {
        var fromRulesPage = fetchRulesPageErrata();
        var fromTopics = fetchTopicsErrata();
        return mergeDedup(fromRulesPage, fromTopics);
    }

    private List<CardErrata> fetchRulesPageErrata() {
        try {
            var document = Jsoup.connect(errataUrl).get();
            return parse(document);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to fetch card errata from " + errataUrl, e);
        }
    }

    private List<CardErrata> fetchTopicsErrata() {
        try {
            var document = Jsoup.connect(topicsUrl).get();
            return selectErrataTopicLinks(document).stream()
                    .flatMap(url -> fetchTopicDetailPage(url).stream())
                    .toList();
        } catch (IOException e) {
            log.warn("Failed to fetch topics feed from {}, skipping topics-sourced errata", topicsUrl, e);
            return List.of();
        }
    }

    private List<CardErrata> fetchTopicDetailPage(String url) {
        try {
            var document = Jsoup.connect(url).get();
            return parseTopicDetailPage(document, url);
        } catch (IOException e) {
            log.warn("Failed to fetch topic notice page {}, skipping", url, e);
            return List.of();
        }
    }

    List<String> selectErrataTopicLinks(Document topicsDocument) {
        return topicsDocument.select("li.topicDetail a[href]").stream()
                .filter(a -> a.absUrl("href").toLowerCase(Locale.ENGLISH).contains("/topics/notice"))
                .filter(a -> {
                    var title = a.selectFirst("span.js_topicTit");
                    return title != null && title.text().toLowerCase(Locale.ENGLISH).contains("errata");
                })
                .map(a -> a.absUrl("href"))
                .distinct()
                .toList();
    }

    List<CardErrata> parseTopicDetailPage(Document document, String pageUrl) {
        var dateElement = document.selectFirst("span.pageTitDate");
        var noticeDate = dateElement != null ? parseDate(dateElement.text()) : null;
        if (noticeDate == null) {
            log.warn("Skipping topic notice page {}, could not resolve a notice date", pageUrl);
            return List.of();
        }

        return document.select("div.contentsWrap div.detailCol").stream()
                .map(detailCol -> toCardErrataFromTopic(detailCol, noticeDate, pageUrl))
                .filter(errata -> errata != null)
                .toList();
    }

    private CardErrata toCardErrataFromTopic(Element detailCol, LocalDate noticeDate, String pageUrl) {
        var cardNumElement = detailCol.selectFirst("p.cardNum");
        if (cardNumElement == null) {
            log.warn("Skipping topic errata entry on {}, no card number found", pageUrl);
            return null;
        }
        var cardCode = cardNumElement.text().trim();

        var cardNameElement = detailCol.selectFirst("h6.cardName");
        var cardName = cardNameElement != null ? textWithLineBreaksAsSpaces(cardNameElement) : null;

        Map<String, String> fields = new LinkedHashMap<>();
        for (var dl : detailCol.select("dl.impNoticeBox")) {
            fields.putAll(extractDlFields(dl));
        }
        var beforeText = fields.get("before");
        var afterText = fields.get("after");

        if (beforeText == null && afterText == null) {
            log.warn("Skipping topic errata entry for {} on {}, no Before/After text found", cardCode, pageUrl);
            return null;
        }

        var noteElement = detailCol.parent() != null ? detailCol.parent().selectFirst("ul.commonNoticeList li") : null;
        var scopeNote = noteElement != null ? NOTE_PREFIX.matcher(noteElement.text()).replaceFirst("").trim() : null;

        return CardErrata.builder()
                .cardCode(cardCode)
                .cardName(cardName)
                .scopeNote(scopeNote)
                .beforeText(beforeText)
                .afterText(afterText)
                .noticeDate(noticeDate)
                .sourceUrl(pageUrl)
                .build();
    }

    List<CardErrata> mergeDedup(List<CardErrata> primary, List<CardErrata> secondary) {
        var seenKeys = primary.stream()
                .map(e -> e.getCardCode() + "|" + e.getNoticeDate())
                .collect(Collectors.toSet());

        var merged = new ArrayList<>(primary);
        for (var errata : secondary) {
            var key = errata.getCardCode() + "|" + errata.getNoticeDate();
            if (seenKeys.add(key)) {
                merged.add(errata);
            }
        }
        return merged;
    }

    List<CardErrata> parse(Document document) {
        return document.select("h5.smallTitRed").stream()
                .map(this::toCardErrata)
                .filter(errata -> errata != null)
                .toList();
    }

    private CardErrata toCardErrata(Element heading) {
        var lines = heading.textNodes().stream()
                .map(TextNode::text)
                .map(String::trim)
                .filter(line -> !line.isEmpty())
                .toList();

        if (lines.isEmpty()) {
            log.warn("Skipping errata entry with no heading text");
            return null;
        }

        var codeAndNameLine = lines.get(lines.size() - 1);
        var matcher = CODE_AND_NAME.matcher(codeAndNameLine);
        if (!matcher.matches()) {
            log.warn("Skipping errata entry, could not parse card code from heading: {}", codeAndNameLine);
            return null;
        }

        var cardCode = matcher.group(1);
        var nameAndSuffix = matcher.group(2).trim();
        String cardName = nameAndSuffix;
        String scopeNote = null;

        var suffixMatcher = TRAILING_PARENTHETICAL.matcher(nameAndSuffix);
        if (suffixMatcher.matches()) {
            cardName = suffixMatcher.group(1).trim();
            scopeNote = suffixMatcher.group(2).trim();
        }

        var noteFromList = heading.parent() != null
                ? heading.parent().selectFirst("ul.commonNoticeList li")
                : null;
        if (noteFromList != null) {
            scopeNote = noteFromList.text().replaceFirst("^\\*\\s*", "").trim();
        }

        var noticeDate = resolveNoticeDate(heading, lines);
        if (noticeDate == null) {
            log.warn("Skipping errata entry for {}, could not resolve a notice date", cardCode);
            return null;
        }

        var dl = heading.parent() != null ? heading.parent().selectFirst("dl") : null;
        var fields = dl != null ? extractDlFields(dl) : Map.<String, String>of();
        var beforeText = fields.get("before");
        var afterText = fields.get("after");

        if (beforeText == null && afterText == null) {
            log.warn("Skipping errata entry for {}, no Before/After text found (image-only fix)", cardCode);
            return null;
        }

        var anchorId = heading.parent() != null ? heading.parent().id() : "";
        var sourceUrl = anchorId.isBlank() ? errataUrl : errataUrl + "#" + anchorId;

        return CardErrata.builder()
                .cardCode(cardCode)
                .cardName(cardName)
                .scopeNote(scopeNote)
                .beforeText(beforeText)
                .afterText(afterText)
                .noticeDate(noticeDate)
                .sourceUrl(sourceUrl)
                .build();
    }

    private LocalDate resolveNoticeDate(Element heading, List<String> lines) {
        if (lines.size() >= 2) {
            return parseDate(lines.get(0));
        }

        var section = heading.closest("section.contentsLCol");
        var dateHeading = section != null ? section.selectFirst("h4.mediumTit") : null;
        return dateHeading != null ? parseDate(dateHeading.text()) : null;
    }

    private LocalDate parseDate(String text) {
        try {
            return LocalDate.parse(text.trim(), DATE_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private Map<String, String> extractDlFields(Element dl) {
        Map<String, String> fields = new LinkedHashMap<>();
        String currentLabel = null;
        for (var child : dl.children()) {
            if (child.tagName().equalsIgnoreCase("dt")) {
                currentLabel = child.text().replace(":", "").trim().toLowerCase(Locale.ENGLISH);
            } else if (child.tagName().equalsIgnoreCase("dd") && currentLabel != null) {
                fields.merge(currentLabel, textWithLineBreaksAsSpaces(child), (a, b) -> a + " " + b);
            }
        }
        return fields;
    }

    private String textWithLineBreaksAsSpaces(Element element) {
        var html = element.html().replaceAll("(?i)<br\\s*/?>", " ");
        return Jsoup.parse(html).text().replaceAll("\\s+", " ").trim();
    }
}

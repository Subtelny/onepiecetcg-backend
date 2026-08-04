package pl.janda.onepiecetcg.cards.application.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;


@Service
public class SemanticQueryParser {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\b(\\d+)(kc|kp|c)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern ERRATA_KEYWORD = Pattern.compile("\\berrata\\b", Pattern.CASE_INSENSITIVE);

    public record ParsedSemanticQuery(String remainingText, Integer cost, Integer counter, Integer power,
                                      boolean errataOnly) {
    }

    public ParsedSemanticQuery parse(String query) {
        if (query == null) {
            return new ParsedSemanticQuery("", null, null, null, false);
        }

        Integer cost = null;
        Integer counter = null;
        Integer power = null;

        var matcher = TOKEN_PATTERN.matcher(query);
        var remaining = new StringBuilder();
        var lastEnd = 0;
        while (matcher.find()) {
            remaining.append(query, lastEnd, matcher.start());
            lastEnd = matcher.end();

            var amount = Integer.parseInt(matcher.group(1));
            var suffix = matcher.group(2).toLowerCase();
            switch (suffix) {
                case "c" -> cost = amount;
                case "kc" -> counter = amount * 1000;
                case "kp" -> power = amount * 1000;
                default -> throw new IllegalStateException("Unexpected suffix: " + suffix);
            }
        }
        remaining.append(query.substring(lastEnd));

        var afterNumericTokens = remaining.toString();
        var errataMatcher = ERRATA_KEYWORD.matcher(afterNumericTokens);
        var errataOnly = errataMatcher.find();
        var afterErrataKeyword = errataOnly ? errataMatcher.replaceAll("") : afterNumericTokens;

        return new ParsedSemanticQuery(afterErrataKeyword.trim().replaceAll("\\s+", " "), cost, counter, power, errataOnly);
    }
}

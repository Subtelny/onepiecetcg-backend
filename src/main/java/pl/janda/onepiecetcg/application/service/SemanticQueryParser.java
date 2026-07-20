package pl.janda.onepiecetcg.application.service;

import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts inline numeric shorthand tokens from a SEMANTIC-mode search query, so a user can type
 * e.g. "rush 6c 2kc" instead of using the cost/counter/power sidebar filters. Suffixes: `c` = cost
 * as-is (6c = cost 6), `kc` = counter in thousands (2kc = counter 2000), `kp` = power in thousands
 * (5kp = power 5000). Whatever text remains after stripping the matched tokens is used as the
 * full-text search query text (see JooqSetCardQueryAdapter's SEMANTIC handling).
 */
@Service
public class SemanticQueryParser {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("\\b(\\d+)(kc|kp|c)\\b", Pattern.CASE_INSENSITIVE);

    public record ParsedSemanticQuery(String remainingText, Integer cost, Integer counter, Integer power) {
    }

    public ParsedSemanticQuery parse(String query) {
        if (query == null) {
            return new ParsedSemanticQuery("", null, null, null);
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

        return new ParsedSemanticQuery(remaining.toString().trim().replaceAll("\\s+", " "), cost, counter, power);
    }
}

package pl.janda.onepiecetcg.cards.application.model;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public final class CardDelimitedValues {

    private static final Pattern VALUE_DELIMITER = Pattern.compile("[,/]+");

    private static final Pattern TOKEN_DELIMITER = Pattern.compile("[\\s,/]+");

    private CardDelimitedValues() {
    }

    public static String normalize(String value) {
        var values = values(value);
        return values.isEmpty() ? null : String.join(", ", values);
    }

    public static List<String> values(String value) {
        return split(value, VALUE_DELIMITER);
    }

    public static List<String> tokens(String value) {
        return split(value, TOKEN_DELIMITER);
    }

    private static List<String> split(String value, Pattern delimiter) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(delimiter.split(value))
                .map(String::trim)
                .map(part -> part.replaceAll("\\s+", " "))
                .filter(part -> !part.isEmpty())
                .distinct()
                .toList();
    }
}

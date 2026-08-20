package pl.janda.onepiecetcg.cards.application.model;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class CardSubTypes {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{L}\\p{N}]+");

    private CardSubTypes() {
    }

    public static String normalize(String value) {
        return CardDelimitedValues.normalize(value);
    }

    public static List<String> values(String value) {
        return CardDelimitedValues.values(value);
    }

    public static Set<String> comparableValues(String value) {
        var comparable = new LinkedHashSet<String>();
        values(value).stream()
                .map(CardSubTypes::comparableValue)
                .filter(subType -> !subType.isEmpty())
                .forEach(comparable::add);
        return comparable;
    }

    public static String comparableValue(String value) {
        if (value == null) {
            return "";
        }
        return NON_ALPHANUMERIC.matcher(value.toLowerCase(Locale.ROOT))
                .replaceAll(" ")
                .trim();
    }
}

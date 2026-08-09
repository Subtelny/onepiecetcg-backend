package pl.janda.onepiecetcg.matchups.application.service;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class LeaderCodeNormalizer {

    // Uppercase-only (no case-insensitive flag): raw source strings prefix the real, always-uppercase
    // card code with a lowercase multiplier separator (e.g. "1xOP14-020"), which a case-insensitive
    // match would otherwise greedily swallow into the captured prefix (matching "xOP14-020").
    private static final Pattern CARD_CODE_PATTERN = Pattern.compile("([A-Z]+\\d+-\\d+)");

    public Optional<String> extractCardCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        var matcher = CARD_CODE_PATTERN.matcher(raw);
        if (!matcher.find()) {
            return Optional.empty();
        }
        return Optional.of(matcher.group(1).toUpperCase(Locale.ROOT));
    }
}

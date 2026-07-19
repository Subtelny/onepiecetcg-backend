package pl.janda.onepiecetcg.application.service;

import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.application.model.SetCard;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class CardEffectExtractionService {

    // Known One Piece TCG keyword abilities, as seen bracketed in optcgapi.com card text.
    // Card text also uses brackets for unrelated card/type/character references (e.g.
    // "[Straw Hat Crew]"), so extraction is restricted to this whitelist to avoid noise.
    private static final Set<String> KNOWN_EFFECTS = Set.of(
            "On Play", "On K.O.", "When Attacking", "Blocker", "Counter", "Trigger",
            "Rush", "Banish", "Double Attack", "Once Per Turn", "Your Turn",
            "Opponent's Turn", "Main", "Activate: Main", "DON!!", "End of Your Turn",
            "On Block", "On Your Opponent's Attack", "Unblockable"
    );

    private static final Map<String, String> KNOWN_EFFECTS_BY_LOWER_CASE = KNOWN_EFFECTS.stream()
            .collect(Collectors.toMap(e -> e.toLowerCase(Locale.ROOT), e -> e));

    private static final Pattern EFFECT_TAG_PATTERN = Pattern.compile("\\[([^\\[\\]]+)]");
    private static final Pattern DON_TAG_PATTERN = Pattern.compile("(?i)^DON!!\\s*[x×]\\s*\\d+$");
    private static final Pattern BARE_COLON_PATTERN = Pattern.compile(":(\\S)");

    public void assignEffects(List<SetCard> cards) {
        cards.forEach(card -> card.setEffects(extract(card.getCardText())));
    }

    private List<String> extract(String cardText) {
        if (cardText == null || cardText.isBlank()) {
            return List.of();
        }

        var matcher = EFFECT_TAG_PATTERN.matcher(cardText);
        var effects = new LinkedHashSet<String>();
        while (matcher.find()) {
            var tag = normalize(matcher.group(1));
            if (tag != null) {
                effects.add(tag);
            }
        }
        return new ArrayList<>(effects);
    }

    private String normalize(String rawTag) {
        var tag = rawTag.trim().replaceAll("\\s+", " ");
        if (DON_TAG_PATTERN.matcher(tag).matches()) {
            return "DON!!";
        }
        tag = BARE_COLON_PATTERN.matcher(tag).replaceAll(": $1");
        return KNOWN_EFFECTS_BY_LOWER_CASE.get(tag.toLowerCase(Locale.ROOT));
    }
}

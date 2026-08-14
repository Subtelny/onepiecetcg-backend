package pl.janda.onepiecetcg.matchups.application.service;

import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.matchups.application.model.MatchupLeaderCardCategory;
import pl.janda.onepiecetcg.matchups.application.model.NormalizedLeaderCard;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class MatchupCardRoleClassifier {

    private static final BigDecimal FULL_PLAYSET_MIN_TYPICAL_COPIES = new BigDecimal("3.5");

    private static final BigDecimal FINISHER_MIN_TYPICAL_COPIES = new BigDecimal("1.0");

    private static final int FINISHER_MIN_COST = 8;

    private static final int GENERIC_COUNTER_AMOUNT = 2000;

    private static final Pattern TYPE_REFERENCE_PATTERN = Pattern.compile("\\{([^{}]+)}");

    public MatchupLeaderCardCategory classify(NormalizedLeaderCard profile, SetCard leader, SetCard card) {
        if (profile.category() != MatchupLeaderCardCategory.EXPECTED) {
            return profile.category();
        }
        if (hasArchetypeAffinity(leader, card)) {
            return MatchupLeaderCardCategory.EXPECTED;
        }
        if (Integer.valueOf(GENERIC_COUNTER_AMOUNT).equals(card.getCounterAmount())) {
            return MatchupLeaderCardCategory.POSSIBLE_TECH;
        }
        if (profile.typicalCopies().compareTo(FULL_PLAYSET_MIN_TYPICAL_COPIES) >= 0) {
            return MatchupLeaderCardCategory.EXPECTED;
        }
        if (isFinisher(card)
                && profile.typicalCopies().compareTo(FINISHER_MIN_TYPICAL_COPIES) >= 0) {
            return MatchupLeaderCardCategory.EXPECTED;
        }
        return MatchupLeaderCardCategory.POSSIBLE_TECH;
    }

    private boolean hasArchetypeAffinity(SetCard leader, SetCard card) {
        var leaderSubTypes = normalize(leader.getSubTypes());
        var cardSubTypes = normalize(card.getSubTypes());
        if (leaderSubTypes.isBlank()) {
            return false;
        }
        if (!cardSubTypes.isBlank()
                && (leaderSubTypes.contains(cardSubTypes) || cardSubTypes.contains(leaderSubTypes))) {
            return true;
        }
        return referencesLeaderType(card.getCardText(), leaderSubTypes)
                || referencesCardType(leader.getCardText(), cardSubTypes);
    }

    private boolean referencesLeaderType(String effect, String leaderSubTypes) {
        var matcher = TYPE_REFERENCE_PATTERN.matcher(effect == null ? "" : effect);
        while (matcher.find()) {
            if (leaderSubTypes.contains(normalize(matcher.group(1)))) {
                return true;
            }
        }
        return false;
    }

    private boolean referencesCardType(String effect, String cardSubTypes) {
        if (cardSubTypes.isBlank()) {
            return false;
        }
        var matcher = TYPE_REFERENCE_PATTERN.matcher(effect == null ? "" : effect);
        while (matcher.find()) {
            if (cardSubTypes.contains(normalize(matcher.group(1)))) {
                return true;
            }
        }
        return false;
    }

    private boolean isFinisher(SetCard card) {
        if (!"Character".equalsIgnoreCase(card.getCardType()) || card.getCardCost() == null) {
            return false;
        }
        try {
            return Integer.parseInt(card.getCardCost().trim()) >= FINISHER_MIN_COST;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{N}]+", " ")
                .trim();
    }
}

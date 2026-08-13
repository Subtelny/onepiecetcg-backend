package pl.janda.onepiecetcg.matchups.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.matchups.application.model.MatchupLeaderCardCategory;
import pl.janda.onepiecetcg.matchups.application.model.NormalizedLeaderCard;
import pl.janda.onepiecetcg.matchups.application.model.RawDecklist;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class MatchupCardProfileService {

    private static final BigDecimal EXPECTED_MIN_INCLUSION_RATE = new BigDecimal("60.00");

    private static final BigDecimal POSSIBLE_TECH_MIN_INCLUSION_RATE = new BigDecimal("10.00");

    private static final int EXPECTED_CARDS_LIMIT = 8;

    private static final int POSSIBLE_TECHS_LIMIT = 5;

    private static final Pattern DECK_CARD_PATTERN = Pattern.compile("(\\d+)x([A-Z]+(?:\\d+)?-\\d+)");

    private static final Comparator<CardCandidate> MOST_COMMON_FIRST =
            Comparator.comparing(CardCandidate::inclusionRate).reversed()
                    .thenComparing(CardCandidate::typicalCopies, Comparator.reverseOrder())
                    .thenComparing(CardCandidate::cardCode);

    private final LeaderCodeNormalizer leaderCodeNormalizer;

    public List<NormalizedLeaderCard> calculateProfiles(List<RawDecklist> rawDecklists,
                                                        Set<String> validLeaderCodes) {
        var leaders = new HashMap<String, LeaderAccumulator>();
        for (var rawDecklist : rawDecklists) {
            var leaderCode = leaderCodeNormalizer.extractCardCode(rawDecklist.getLeader()).orElse(null);
            if (leaderCode == null) {
                log.warn("Dropping decklist with unparseable leader '{}'", rawDecklist.getLeader());
                continue;
            }
            if (!validLeaderCodes.contains(leaderCode) || rawDecklist.getGames() == null || rawDecklist.getGames() <= 0) {
                continue;
            }

            var leader = leaders.computeIfAbsent(leaderCode, ignored -> new LeaderAccumulator());
            leader.totalGames += rawDecklist.getGames();
            for (var entry : parseDeck(rawDecklist.getDeck()).entrySet()) {
                if (leaderCode.equals(entry.getKey())) {
                    continue;
                }
                var card = leader.cards.computeIfAbsent(entry.getKey(), ignored -> new CardAccumulator());
                card.includedGames += rawDecklist.getGames();
                card.weightedCopies = card.weightedCopies.add(
                        BigDecimal.valueOf(rawDecklist.getGames()).multiply(BigDecimal.valueOf(entry.getValue())));
            }
        }

        var profiles = new ArrayList<NormalizedLeaderCard>();
        leaders.forEach((leaderCode, accumulator) -> profiles.addAll(toProfile(leaderCode, accumulator)));
        return profiles;
    }

    private List<NormalizedLeaderCard> toProfile(String leaderCode, LeaderAccumulator accumulator) {
        if (accumulator.totalGames <= 0) {
            return List.of();
        }

        var candidates = accumulator.cards.entrySet().stream()
                .map(entry -> toCandidate(leaderCode, entry.getKey(), entry.getValue(), accumulator.totalGames))
                .toList();

        var expectedCards = candidates.stream()
                .filter(card -> card.inclusionRate().compareTo(EXPECTED_MIN_INCLUSION_RATE) >= 0)
                .sorted(MOST_COMMON_FIRST)
                .limit(EXPECTED_CARDS_LIMIT)
                .map(card -> withCategory(card, MatchupLeaderCardCategory.EXPECTED))
                .toList();

        var possibleTechs = candidates.stream()
                .filter(card -> card.inclusionRate().compareTo(POSSIBLE_TECH_MIN_INCLUSION_RATE) >= 0)
                .filter(card -> card.inclusionRate().compareTo(EXPECTED_MIN_INCLUSION_RATE) < 0)
                .sorted(MOST_COMMON_FIRST)
                .limit(POSSIBLE_TECHS_LIMIT)
                .map(card -> withCategory(card, MatchupLeaderCardCategory.POSSIBLE_TECH))
                .toList();

        var profile = new ArrayList<NormalizedLeaderCard>(expectedCards.size() + possibleTechs.size());
        profile.addAll(expectedCards);
        profile.addAll(possibleTechs);
        return profile;
    }

    private CardCandidate toCandidate(String leaderCode, String cardCode, CardAccumulator accumulator,
                                      long totalGames) {
        var inclusionRate = BigDecimal.valueOf(accumulator.includedGames)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalGames), 2, RoundingMode.HALF_UP);
        var typicalCopies = accumulator.weightedCopies
                .divide(BigDecimal.valueOf(accumulator.includedGames), 1, RoundingMode.HALF_UP);
        return new CardCandidate(leaderCode, cardCode, inclusionRate, typicalCopies);
    }

    private NormalizedLeaderCard withCategory(CardCandidate card, MatchupLeaderCardCategory category) {
        return new NormalizedLeaderCard(card.leaderCode(), card.cardCode(), category,
                card.inclusionRate(), card.typicalCopies());
    }

    private Map<String, Integer> parseDeck(String rawDeck) {
        var cards = new HashMap<String, Integer>();
        if (rawDeck == null || rawDeck.isBlank()) {
            return cards;
        }
        var matcher = DECK_CARD_PATTERN.matcher(rawDeck);
        while (matcher.find()) {
            try {
                var copies = Integer.parseInt(matcher.group(1));
                if (copies > 0) {
                    cards.merge(matcher.group(2), copies, Integer::sum);
                }
            } catch (NumberFormatException ignored) {
                // Ignore a malformed entry while preserving the rest of the source decklist.
            }
        }
        return cards;
    }

    private static class LeaderAccumulator {
        private final Map<String, CardAccumulator> cards = new HashMap<>();
        private long totalGames;
    }

    private static class CardAccumulator {
        private long includedGames;
        private BigDecimal weightedCopies = BigDecimal.ZERO;
    }

    private record CardCandidate(String leaderCode, String cardCode, BigDecimal inclusionRate,
                                 BigDecimal typicalCopies) {
    }
}

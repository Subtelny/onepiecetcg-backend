package pl.janda.onepiecetcg.matchups.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.matchups.application.model.MatchupLeaderCardCategory;
import pl.janda.onepiecetcg.matchups.application.model.NormalizedLeaderCard;
import pl.janda.onepiecetcg.matchups.application.model.RawDecklist;
import pl.janda.onepiecetcg.matchups.application.model.RepresentativeDeck;

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

    private static final int EXPECTED_CARDS_LIMIT = 20;

    private static final int POSSIBLE_TECHS_LIMIT = 20;

    private static final int OBSERVED_CARDS_LIMIT = EXPECTED_CARDS_LIMIT + POSSIBLE_TECHS_LIMIT;

    private static final int MIN_RELIABLE_DECKLISTS = 3;

    private static final int MIN_HIGH_PERFORMING_DECKLISTS = 5;

    private static final Pattern DECK_CARD_PATTERN = Pattern.compile("(\\d+)x([A-Z]+(?:\\d+)?-\\d+)");

    private static final Comparator<CardCandidate> MOST_COMMON_FIRST =
            Comparator.comparing(CardCandidate::inclusionRate).reversed()
                    .thenComparing(CardCandidate::typicalCopies, Comparator.reverseOrder())
                    .thenComparing(CardCandidate::cardCode);

    private final LeaderCodeNormalizer leaderCodeNormalizer;

    public List<NormalizedLeaderCard> calculateProfiles(List<RawDecklist> rawDecklists,
                                                        Set<String> validLeaderCodes) {
        var decklistsByLeader = groupValidDecklistsByLeader(rawDecklists, validLeaderCodes);

        var profiles = new ArrayList<NormalizedLeaderCard>();
        decklistsByLeader.forEach((leaderCode, leaderDecklists) -> {
            var selectedDecklists = selectRepresentativeDecklists(leaderCode, leaderDecklists);
            var accumulator = new LeaderAccumulator();
            accumulator.decklistCount = selectedDecklists.size();
            for (var rawDecklist : selectedDecklists) {
                accumulator.totalGames += rawDecklist.getGames();
                for (var entry : parseDeck(rawDecklist.getDeck()).entrySet()) {
                    if (leaderCode.equals(entry.getKey())) {
                        continue;
                    }
                    var card = accumulator.cards.computeIfAbsent(entry.getKey(), ignored -> new CardAccumulator());
                    card.includedGames += rawDecklist.getGames();
                    card.weightedCopies = card.weightedCopies.add(
                            BigDecimal.valueOf(rawDecklist.getGames()).multiply(BigDecimal.valueOf(entry.getValue())));
                }
            }
            profiles.addAll(toProfile(leaderCode, accumulator));
        });
        return profiles;
    }

    public List<RepresentativeDeck> calculateRepresentativeDecks(List<RawDecklist> rawDecklists,
                                                                 Set<String> validLeaderCodes) {
        var representativeDecks = new ArrayList<RepresentativeDeck>();
        groupValidDecklistsByLeader(rawDecklists, validLeaderCodes).forEach((leaderCode, leaderDecklists) -> {
            var completeDecklists = leaderDecklists.stream()
                    .filter(decklist -> isCompleteDeck(leaderCode, decklist))
                    .toList();
            if (completeDecklists.isEmpty()) {
                log.warn("No complete 50-card decklist found for leader '{}'", leaderCode);
                return;
            }

            selectRepresentativeDecklists(leaderCode, completeDecklists).stream()
                    .max(Comparator.comparing(RawDecklist::getGames)
                            .thenComparing(RawDecklist::getWinRate)
                            .thenComparing(RawDecklist::getDeck))
                    .map(decklist -> toRepresentativeDeck(leaderCode, decklist))
                    .ifPresent(representativeDecks::add);
        });
        return representativeDecks.stream()
                .sorted(Comparator.comparing(RepresentativeDeck::leaderCode))
                .toList();
    }

    private Map<String, List<RawDecklist>> groupValidDecklistsByLeader(List<RawDecklist> rawDecklists,
                                                                       Set<String> validLeaderCodes) {
        var decklistsByLeader = new HashMap<String, List<RawDecklist>>();
        for (var rawDecklist : rawDecklists) {
            var leaderCode = leaderCodeNormalizer.extractCardCode(rawDecklist.getLeader()).orElse(null);
            if (leaderCode == null) {
                log.warn("Dropping decklist with unparseable leader '{}'", rawDecklist.getLeader());
                continue;
            }
            if (!validLeaderCodes.contains(leaderCode)
                    || rawDecklist.getGames() == null
                    || rawDecklist.getGames() <= 0
                    || rawDecklist.getWinRate() == null) {
                continue;
            }
            decklistsByLeader.computeIfAbsent(leaderCode, ignored -> new ArrayList<>()).add(rawDecklist);
        }
        return decklistsByLeader;
    }

    private boolean isCompleteDeck(String leaderCode, RawDecklist rawDecklist) {
        var cards = new HashMap<>(parseDeck(rawDecklist.getDeck()));
        cards.remove(leaderCode);
        return cards.values().stream().allMatch(copies -> copies >= 1 && copies <= 4)
                && cards.values().stream().mapToInt(Integer::intValue).sum() == 50;
    }

    private RepresentativeDeck toRepresentativeDeck(String leaderCode, RawDecklist rawDecklist) {
        var cards = new TreeMap<>(parseDeck(rawDecklist.getDeck()));
        cards.remove(leaderCode);
        return new RepresentativeDeck(leaderCode, Map.copyOf(cards), rawDecklist.getGames(), rawDecklist.getWinRate());
    }

    private List<RawDecklist> selectRepresentativeDecklists(String leaderCode, List<RawDecklist> decklists) {
        var orderedByWinRate = decklists.stream()
                .sorted(Comparator.comparing(RawDecklist::getWinRate).reversed())
                .toList();
        var upperQuartileSize = (orderedByWinRate.size() + 3) / 4;
        var upperQuartileCutoff = orderedByWinRate.get(upperQuartileSize - 1).getWinRate();
        var highPerforming = orderedByWinRate.stream()
                .filter(decklist -> decklist.getWinRate().compareTo(upperQuartileCutoff) >= 0)
                .toList();
        if (highPerforming.size() < MIN_HIGH_PERFORMING_DECKLISTS) {
            return decklists;
        }
        log.debug("Using {} of {} high-performing decklists for leader '{}' (dynamic cutoff: {}%)",
                highPerforming.size(), decklists.size(), leaderCode, upperQuartileCutoff);
        return highPerforming;
    }

    private List<NormalizedLeaderCard> toProfile(String leaderCode, LeaderAccumulator accumulator) {
        if (accumulator.totalGames <= 0) {
            return List.of();
        }

        var candidates = accumulator.cards.entrySet().stream()
                .map(entry -> toCandidate(leaderCode, entry.getKey(), entry.getValue(), accumulator.totalGames))
                .toList();

        if (accumulator.decklistCount < MIN_RELIABLE_DECKLISTS) {
            return candidates.stream()
                    .filter(card -> card.inclusionRate().compareTo(POSSIBLE_TECH_MIN_INCLUSION_RATE) >= 0)
                    .sorted(MOST_COMMON_FIRST)
                    .limit(OBSERVED_CARDS_LIMIT)
                    .map(card -> withCategory(card, MatchupLeaderCardCategory.OBSERVED, accumulator.decklistCount))
                    .toList();
        }

        var expectedCards = candidates.stream()
                .filter(card -> card.inclusionRate().compareTo(EXPECTED_MIN_INCLUSION_RATE) >= 0)
                .sorted(MOST_COMMON_FIRST)
                .limit(EXPECTED_CARDS_LIMIT)
                .map(card -> withCategory(card, MatchupLeaderCardCategory.EXPECTED, accumulator.decklistCount))
                .toList();

        var possibleTechs = candidates.stream()
                .filter(card -> card.inclusionRate().compareTo(POSSIBLE_TECH_MIN_INCLUSION_RATE) >= 0)
                .filter(card -> card.inclusionRate().compareTo(EXPECTED_MIN_INCLUSION_RATE) < 0)
                .sorted(MOST_COMMON_FIRST)
                .limit(POSSIBLE_TECHS_LIMIT)
                .map(card -> withCategory(card, MatchupLeaderCardCategory.POSSIBLE_TECH, accumulator.decklistCount))
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

    private NormalizedLeaderCard withCategory(CardCandidate card, MatchupLeaderCardCategory category, int sampleSize) {
        return new NormalizedLeaderCard(card.leaderCode(), card.cardCode(), category,
                card.inclusionRate(), card.typicalCopies(), sampleSize);
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
        private int decklistCount;
    }

    private static class CardAccumulator {
        private long includedGames;
        private BigDecimal weightedCopies = BigDecimal.ZERO;
    }

    private record CardCandidate(String leaderCode, String cardCode, BigDecimal inclusionRate,
                                 BigDecimal typicalCopies) {
    }
}

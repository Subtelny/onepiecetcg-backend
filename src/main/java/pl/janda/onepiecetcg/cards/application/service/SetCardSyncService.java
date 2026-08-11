package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.cards.application.model.OnePieceCard;
import pl.janda.onepiecetcg.cards.application.model.SetCard;
import pl.janda.onepiecetcg.cards.application.port.in.SetCardSyncUseCase;
import pl.janda.onepiecetcg.cards.application.repository.OnePieceCardRepository;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;


@Service
@RequiredArgsConstructor
@Slf4j
public class SetCardSyncService implements SetCardSyncUseCase {

    private static final String PROMOTION_CARD_SET_ID = "569901";

    private static final Pattern VARIANT_CARD_ID = Pattern.compile("^(.+)_([pr][1-9]\\d*)$", Pattern.CASE_INSENSITIVE);

    private static final Map<String, String> RARITY_CODES = Map.ofEntries(
            Map.entry("common", "C"),
            Map.entry("uncommon", "UC"),
            Map.entry("rare", "R"),
            Map.entry("super rare", "SR"),
            Map.entry("leader", "L"),
            Map.entry("secret rare", "SEC"),
            Map.entry("treasure rare", "TR"),
            Map.entry("special", "SP"),
            Map.entry("promo", "PR")
    );

    private final OnePieceCardRepository onePieceCardRepository;

    private final FlatRarityCalculatorService flatRarityCalculatorService;

    private final SetCardReplacementService setCardReplacementService;

    @Override
    public void syncSetCards() {
        var startTime = System.currentTimeMillis();
        log.info("Set cards sync started");

        log.info("Loading all cards from onepiece_cards");
        var loadStartTime = System.currentTimeMillis();
        var fetched = onePieceCardRepository.findAll().stream()
                .map(this::toSetCard)
                .toList();
        var loadDuration = System.currentTimeMillis() - loadStartTime;
        log.info("Loaded and mapped {} cards from onepiece_cards in {}ms", fetched.size(), loadDuration);

        if (fetched.isEmpty()) {
            throw new IllegalStateException("onepiece_cards is empty; refusing to replace set_cards");
        }

        log.info("Setting sync timestamp on fetched cards");
        var now = LocalDateTime.now();
        fetched.forEach(setCard -> setCard.setLastSyncedAt(now));

        log.info("Assigning flat rarities to {} cards", fetched.size());
        var rarityStartTime = System.currentTimeMillis();
        flatRarityCalculatorService.assignFlatRarities(fetched);
        var rarityDuration = System.currentTimeMillis() - rarityStartTime;
        log.info("Assigned flat rarities in {}ms", rarityDuration);

        setCardReplacementService.replaceAll(fetched);

        var totalDuration = System.currentTimeMillis() - startTime;
        log.info("Set cards sync completed successfully - Total time: {}ms ({} seconds), of which load={}ms, rarity={}ms",
                totalDuration, totalDuration / 1000, loadDuration, rarityDuration);
    }


    @Async
    @Override
    public void syncSetCardsAsync() {
        log.info("Starting async set cards sync in separate thread");
        try {
            syncSetCards();
            log.info("Async set cards sync completed successfully");
        } catch (Exception e) {
            log.error("Error during async set cards sync", e);
        }
    }

    private static String extractPrefix(String cardCode) {
        if (cardCode == null) {
            return null;
        }
        var separator = cardCode.indexOf('-');
        return separator > 0 ? cardCode.substring(0, separator) : null;
    }

    private String normalizeRarity(String rarity) {
        if (rarity == null || rarity.isBlank()) {
            return null;
        }
        var normalized = RARITY_CODES.get(rarity.trim().toLowerCase(Locale.ROOT));
        if (normalized == null) {
            log.warn("Unknown rarity '{}' in onepiece_cards", rarity);
        }
        return normalized;
    }

    private static String combineCardText(String effect, String trigger) {
        var normalizedEffect = blankToNull(effect);
        var normalizedTrigger = blankToNull(trigger);
        if (normalizedTrigger == null) {
            return normalizedEffect;
        }
        var triggerText = normalizedTrigger.regionMatches(true, 0, "[Trigger]", 0, "[Trigger]".length())
                ? normalizedTrigger
                : "[Trigger] " + normalizedTrigger;
        return normalizedEffect == null ? triggerText : normalizedEffect + "\n" + triggerText;
    }

    private static String normalizeList(String value) {
        var normalized = blankToNull(value);
        return normalized == null ? null : normalized.replaceAll("[,/]+", " ").replaceAll("\\s+", " ").trim();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String firstNonBlank(String preferred, String fallback) {
        var normalized = blankToNull(preferred);
        return normalized != null ? normalized : blankToNull(fallback);
    }

    private static String extractVariantIndex(String sourceId, String cardCode) {
        var normalizedSourceId = blankToNull(sourceId);
        if (normalizedSourceId == null || normalizedSourceId.equalsIgnoreCase(cardCode)) {
            return SetCard.DEFAULT_VARIANT_INDEX;
        }

        var matcher = VARIANT_CARD_ID.matcher(normalizedSourceId);
        if (matcher.matches() && matcher.group(1).equalsIgnoreCase(cardCode)) {
            return matcher.group(2).toLowerCase(Locale.ROOT);
        }
        throw new IllegalArgumentException(
                "Unsupported onepiece_cards variant id '" + sourceId + "' for card code '" + cardCode + "'");
    }

    private SetCard toSetCard(OnePieceCard source) {
        var cardCode = firstNonBlank(source.getBaseId(), source.getId());
        var leader = "Leader".equalsIgnoreCase(source.getCategory());
        var promo = PROMOTION_CARD_SET_ID.equals(source.getSetId())
                || "Promotion card".equalsIgnoreCase(source.getSetName());
        var sourceRarity = normalizeRarity(source.getRarity());

        return SetCard.builder()
                .cardSetId(cardCode)
                .cardPrefix(extractPrefix(cardCode))
                .cardName(source.getName())
                .setId(source.getSetId())
                .setName(source.getSetName())
                .cardText(combineCardText(source.getEffect(), source.getTrigger()))
                .rarity(promo ? "PR" : sourceRarity)
                .flatRarity(promo && !"PR".equals(sourceRarity) ? sourceRarity : null)
                .cardColor(normalizeList(source.getColors()))
                .cardType(source.getCategory())
                .life(leader ? asString(source.getCost()) : null)
                .cardCost(leader ? null : asString(source.getCost()))
                .cardPower(asString(source.getPower()))
                .subTypes(normalizeList(source.getTypes()))
                .counterAmount(source.getCounter())
                .attribute(normalizeList(source.getAttributes()))
                .cardImageId(source.getId())
                .cardImage(source.getImageUrl())
                .variantIndex(extractVariantIndex(source.getId(), cardCode))
                .build();
    }

    private static String asString(Integer value) {
        return value != null ? value.toString() : null;
    }
}

package pl.janda.onepiecetcg.deckbuilder.rest.mapper;

import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.cards.application.model.*;
import pl.janda.onepiecetcg.deckbuilder.rest.dto.*;
import pl.janda.onepiecetcg.pricing.application.model.PriceQuote;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class DeckBuilderCardMapper {

    private static String displayNameOrCardName(String displayName, String cardName) {
        return displayName == null || displayName.isBlank() ? cardName : displayName;
    }

    public List<DeckBuilderCardDto> toDtoList(List<SetCard> cards) {
        return toDtoList(cards, Map.of());
    }

    public List<DeckBuilderCardDto> toDtoList(
            List<SetCard> cards,
            Map<String, List<PriceQuote>> pricesByReference
    ) {
        if (cards == null) {
            return List.of();
        }
        return cards.stream()
                .map(card -> toDto(card, getPrices(card.getPriceReference(), pricesByReference)))
                .collect(Collectors.toList());
    }

    public DeckBuilderCardDto toDto(SetCard card) {
        return toDto(card, List.of());
    }

    public DeckBuilderCardDto toDto(SetCard card, List<PriceQuote> prices) {
        if (card == null) {
            return null;
        }

        return DeckBuilderCardDto.builder()
                .id(card.getId() != null ? String.valueOf(card.getId()) : null)
                .name(card.getCardName())
                .displayName(displayNameOrCardName(card.getDisplayName(), card.getCardName()))
                .sourceProduct(card.getSourceProduct())
                .released(card.isReleased())
                .releaseDate(card.getReleaseDate())
                .variantIndex(card.getVariantIndex())
                .type(parseCardType(card.getCardType()))
                .color(parseColors(card.getCardColor()))
                .cost(parseIntSafe(card.getCardCost()))
                .power(parseIntSafe(card.getCardPower()))
                .counter(card.getCounterAmount())
                .attribute(parseAttributes(card.getAttribute()))
                .effect(card.getCardText())
                .rarity(card.getRarity())
                .flatRarity(card.getFlatRarity())
                .cardNumber(card.getCardSetId())
                .imageUrl(card.getCardImage())
                .marketPrice(card.getMarketPrice())
                .inventoryPrice(card.getInventoryPrice())
                .prices(toPriceDtoList(prices))
                .build();
    }

    public List<DeckBuilderCardSummaryDto> toSummaryDtoList(List<CardSummary> cards) {
        return toSummaryDtoList(cards, Map.of());
    }

    public List<DeckBuilderCardSummaryDto> toSummaryDtoList(
            List<CardSummary> cards,
            Map<String, List<PriceQuote>> pricesByReference
    ) {
        if (cards == null) {
            return List.of();
        }
        return cards.stream()
                .map(card -> toSummaryDto(card, getPrices(card.getPriceReference(), pricesByReference)))
                .toList();
    }

    public DeckBuilderCardSummaryDto toSummaryDto(CardSummary card) {
        return toSummaryDto(card, List.of());
    }

    public DeckBuilderCardSummaryDto toSummaryDto(CardSummary card, List<PriceQuote> prices) {
        if (card == null) {
            return null;
        }
        return DeckBuilderCardSummaryDto.builder()
                .id(card.getId() != null ? String.valueOf(card.getId()) : null)
                .name(card.getCardName())
                .displayName(displayNameOrCardName(card.getDisplayName(), card.getCardName()))
                .sourceProduct(card.getSourceProduct())
                .released(card.isReleased())
                .releaseDate(card.getReleaseDate())
                .variantIndex(card.getVariantIndex())
                .cardNumber(card.getCardSetId())
                .flatRarity(card.getFlatRarity())
                .imageUrl(card.getCardImage())
                .prices(toPriceDtoList(prices))
                .build();
    }

    private List<DeckBuilderCardPriceDto> toPriceDtoList(List<PriceQuote> prices) {
        if (prices == null) {
            return List.of();
        }
        return prices.stream()
                .map(price -> DeckBuilderCardPriceDto.builder()
                        .source(price.getSource() != null ? price.getSource().name() : null)
                        .currency(price.getCurrency())
                        .productId(price.getExternalProductId())
                        .productName(price.getProductName())
                        .averagePrice(price.getAveragePrice())
                        .lowPrice(price.getLowPrice())
                        .trendPrice(price.getTrendPrice())
                        .observedAt(price.getObservedAt() != null
                                ? DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(price.getObservedAt())
                                : null)
                        .build())
                .toList();
    }

    private List<PriceQuote> getPrices(
            String priceReference,
            Map<String, List<PriceQuote>> pricesByReference
    ) {
        if (priceReference == null || pricesByReference == null) {
            return List.of();
        }
        return pricesByReference.getOrDefault(priceReference, List.of());
    }

    public DeckBuilderCardFilterOptionsDto toFilterOptionsDto(CardFilterOptions options) {
        return DeckBuilderCardFilterOptionsDto.builder()
                .types(options.getTypes())
                .colors(options.getColors())
                .rarities(options.getRarities())
                .flatRarities(options.getFlatRarities())
                .costs(options.getCosts())
                .sets(options.getSets().stream()
                        .map(s -> DeckBuilderCardSetOptionDto.builder()
                                .setId(s.getSetId())
                                .setName(s.getSetName())
                                .released(s.isReleased())
                                .releaseDate(s.getReleaseDate())
                                .build())
                        .toList())
                .attributes(options.getAttributes())
                .attributeCombos(options.getAttributeCombos())
                .subTypes(options.getSubTypes())
                .prefixes(options.getPrefixes())
                .build();
    }

    private String parseCardType(String cardType) {
        if (cardType == null) {
            return null;
        }
        try {
            return CardType.valueOf(cardType.toUpperCase()).name();
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private List<String> parseColors(String cardColor) {
        return CardDelimitedValues.tokens(cardColor).stream()
                .map(String::toUpperCase)
                .filter(token -> Arrays.stream(CardColor.values()).anyMatch(c -> c.name().equals(token)))
                .toList();
    }

    private List<String> parseAttributes(String attribute) {
        return CardDelimitedValues.tokens(attribute).stream()
                .filter(token -> !token.isBlank() && !token.equalsIgnoreCase("null"))
                .toList();
    }

    private Integer parseIntSafe(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

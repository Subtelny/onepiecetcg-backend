package pl.janda.onepiecetcg.cards.rest.mapper;

import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.cards.application.model.*;
import pl.janda.onepiecetcg.cards.rest.dto.*;
import pl.janda.onepiecetcg.pricing.application.model.PriceHistoryPoint;
import pl.janda.onepiecetcg.pricing.application.model.PriceQuote;

import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class CardMapper {

    public CardDto toDto(SetCard card) {
        return toDto(card, List.of(), List.of(), List.of());
    }

    private static String displayNameOrCardName(String displayName, String cardName) {
        return displayName == null || displayName.isBlank() ? cardName : displayName;
    }

    public List<CardDto> toDtoList(List<SetCard> cards) {
        return toDtoList(cards, Map.of(), Map.of(), Map.of());
    }

    public List<CardDto> toDtoList(List<SetCard> cards, Map<String, List<CardErrata>> errataByCardCode,
                                    Map<String, List<CardFaq>> faqByCardCode) {
        return toDtoList(cards, errataByCardCode, faqByCardCode, Map.of());
    }

    public List<CardDto> toDtoList(
            List<SetCard> cards,
            Map<String, List<CardErrata>> errataByCardCode,
            Map<String, List<CardFaq>> faqByCardCode,
            Map<String, List<PriceQuote>> pricesByReference
    ) {
        if (cards == null) {
            return List.of();
        }
        return cards.stream()
                .map(card -> toDto(
                        card,
                        errataByCardCode.get(card.getCardSetId()),
                        faqByCardCode.get(card.getCardSetId()),
                        getPrices(card.getPriceReference(), pricesByReference)))
                .collect(Collectors.toList());
    }

    public CardDto toDto(SetCard card, List<CardErrata> errataHistory, List<CardFaq> faqHistory) {
        return toDto(card, errataHistory, faqHistory, List.of());
    }

    public CardDto toDto(
            SetCard card,
            List<CardErrata> errataHistory,
            List<CardFaq> faqHistory,
            List<PriceQuote> prices
    ) {
        return toDto(card, errataHistory, faqHistory, prices, List.of());
    }

    public CardDto toDto(
            SetCard card,
            List<CardErrata> errataHistory,
            List<CardFaq> faqHistory,
            List<PriceQuote> prices,
            List<PriceHistoryPoint> priceHistory
    ) {
        if (card == null) {
            return null;
        }

        return CardDto.builder()
                .id(card.getId() != null ? String.valueOf(card.getId()) : null)
                .name(card.getCardName())
                .displayName(displayNameOrCardName(card.getDisplayName(), card.getCardName()))
                .sourceProduct(card.getSourceProduct())
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
                .priceHistory(toPriceHistoryDtoList(priceHistory))
                .errata(toErrataEntryDtoList(errataHistory))
                .faq(toFaqEntryDtoList(faqHistory))
                .build();
    }

    public List<CardSummaryDto> toSummaryDtoList(List<CardSummary> cards) {
        return toSummaryDtoList(cards, Map.of());
    }

    public List<CardSummaryDto> toSummaryDtoList(
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

    public CardSummaryDto toSummaryDto(CardSummary card) {
        return toSummaryDto(card, List.of());
    }

    public CardSummaryDto toSummaryDto(CardSummary card, List<PriceQuote> prices) {
        if (card == null) {
            return null;
        }
        return CardSummaryDto.builder()
                .id(card.getId() != null ? String.valueOf(card.getId()) : null)
                .name(card.getCardName())
                .displayName(displayNameOrCardName(card.getDisplayName(), card.getCardName()))
                .sourceProduct(card.getSourceProduct())
                .cardNumber(card.getCardSetId())
                .flatRarity(card.getFlatRarity())
                .imageUrl(card.getCardImage())
                .variantIndex(card.getVariantIndex())
                .prices(toPriceDtoList(prices))
                .build();
    }

    private List<CardPriceDto> toPriceDtoList(List<PriceQuote> prices) {
        if (prices == null) {
            return List.of();
        }
        return prices.stream()
                .map(price -> CardPriceDto.builder()
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

    private List<CardPriceHistoryPointDto> toPriceHistoryDtoList(List<PriceHistoryPoint> history) {
        if (history == null) {
            return List.of();
        }
        return history.stream()
                .map(point -> CardPriceHistoryPointDto.builder()
                        .source(point.getSource() != null ? point.getSource().name() : null)
                        .currency(point.getCurrency())
                        .observedAt(point.getObservedAt() != null
                                ? DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(point.getObservedAt())
                                : null)
                        .trendPrice(point.getTrendPrice())
                        .lowPrice(point.getLowPrice())
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

    private List<CardErrataEntryDto> toErrataEntryDtoList(List<CardErrata> errataHistory) {
        if (errataHistory == null) {
            return List.of();
        }
        return errataHistory.stream()
                .map(e -> CardErrataEntryDto.builder()
                        .date(e.getNoticeDate() != null ? e.getNoticeDate().toString() : null)
                        .before(e.getBeforeText())
                        .after(e.getAfterText())
                        .note(e.getScopeNote())
                        .build())
                .toList();
    }

    private List<CardFaqEntryDto> toFaqEntryDtoList(List<CardFaq> faqHistory) {
        if (faqHistory == null) {
            return List.of();
        }
        return faqHistory.stream()
                .map(f -> CardFaqEntryDto.builder()
                        .question(f.getQuestion())
                        .answer(f.getAnswer())
                        .build())
                .toList();
    }

    public CardFilterOptionsDto toFilterOptionsDto(CardFilterOptions options) {
        return CardFilterOptionsDto.builder()
                .types(options.getTypes())
                .colors(options.getColors())
                .rarities(options.getRarities())
                .flatRarities(options.getFlatRarities())
                .costs(options.getCosts())
                .sets(options.getSets().stream()
                        .map(s -> CardSetOptionDto.builder().setId(s.getSetId()).setName(s.getSetName()).build())
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

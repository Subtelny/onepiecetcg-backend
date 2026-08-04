package pl.janda.onepiecetcg.deckbuilder.rest.mapper;

import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.cards.application.model.*;
import pl.janda.onepiecetcg.deckbuilder.rest.dto.DeckBuilderCardDto;
import pl.janda.onepiecetcg.deckbuilder.rest.dto.DeckBuilderCardFilterOptionsDto;
import pl.janda.onepiecetcg.deckbuilder.rest.dto.DeckBuilderCardSetOptionDto;
import pl.janda.onepiecetcg.deckbuilder.rest.dto.DeckBuilderCardSummaryDto;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DeckBuilderCardMapper {

    public DeckBuilderCardDto toDto(SetCard card) {
        if (card == null) {
            return null;
        }

        return DeckBuilderCardDto.builder()
                .id(card.getId() != null ? String.valueOf(card.getId()) : null)
                .name(card.getCardName())
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
                .build();
    }

    public List<DeckBuilderCardDto> toDtoList(List<SetCard> cards) {
        if (cards == null) {
            return List.of();
        }
        return cards.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public DeckBuilderCardSummaryDto toSummaryDto(CardSummary card) {
        if (card == null) {
            return null;
        }
        return DeckBuilderCardSummaryDto.builder()
                .id(card.getId() != null ? String.valueOf(card.getId()) : null)
                .name(card.getCardName())
                .cardNumber(card.getCardSetId())
                .flatRarity(card.getFlatRarity())
                .imageUrl(card.getCardImage())
                .build();
    }

    public List<DeckBuilderCardSummaryDto> toSummaryDtoList(List<CardSummary> cards) {
        if (cards == null) {
            return List.of();
        }
        return cards.stream()
                .map(this::toSummaryDto)
                .toList();
    }

    public DeckBuilderCardFilterOptionsDto toFilterOptionsDto(CardFilterOptions options) {
        return DeckBuilderCardFilterOptionsDto.builder()
                .types(options.getTypes())
                .colors(options.getColors())
                .rarities(options.getRarities())
                .flatRarities(options.getFlatRarities())
                .costs(options.getCosts())
                .sets(options.getSets().stream()
                        .map(s -> DeckBuilderCardSetOptionDto.builder().setId(s.getSetId()).setName(s.getSetName()).build())
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
        if (cardColor == null) {
            return List.of();
        }
        return Arrays.stream(cardColor.split("\\s+"))
                .map(String::toUpperCase)
                .filter(token -> Arrays.stream(CardColor.values()).anyMatch(c -> c.name().equals(token)))
                .toList();
    }

    private List<String> parseAttributes(String attribute) {
        if (attribute == null || attribute.isBlank()) {
            return List.of();
        }
        return Arrays.stream(attribute.split("[\\s/]+"))
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

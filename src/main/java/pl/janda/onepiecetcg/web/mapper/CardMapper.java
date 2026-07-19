package pl.janda.onepiecetcg.web.mapper;

import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.application.model.CardColor;
import pl.janda.onepiecetcg.application.model.CardFilterOptions;
import pl.janda.onepiecetcg.application.model.CardType;
import pl.janda.onepiecetcg.application.model.SetCard;
import pl.janda.onepiecetcg.web.dto.CardDto;
import pl.janda.onepiecetcg.web.dto.CardFilterOptionsDto;
import pl.janda.onepiecetcg.web.dto.CardSetOptionDto;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CardMapper {

    public CardDto toDto(SetCard card) {
        if (card == null) {
            return null;
        }

        return CardDto.builder()
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

    public List<CardDto> toDtoList(List<SetCard> cards) {
        return cards != null ?
                cards.stream().map(this::toDto).collect(Collectors.toList()) : List.of();
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

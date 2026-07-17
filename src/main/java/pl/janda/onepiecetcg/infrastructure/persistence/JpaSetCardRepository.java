package pl.janda.onepiecetcg.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.application.model.CardColor;
import pl.janda.onepiecetcg.application.model.CardRarity;
import pl.janda.onepiecetcg.application.model.CardType;
import pl.janda.onepiecetcg.application.model.SetCard;
import pl.janda.onepiecetcg.application.repository.SetCardRepository;

import java.util.Arrays;
import java.util.List;

@Repository
public interface JpaSetCardRepository extends JpaRepository<SetCard, Long>, SetCardRepository {

    @Override
    default List<SetCard> search(
            String name,
            CardType type,
            List<CardColor> colors,
            List<CardRarity> rarities,
            Integer cost,
            Integer power,
            String setId,
            Integer counterAmount,
            String attribute,
            String subTypes
    ) {
        return findAll().stream()
                .filter(c -> name == null ||
                        (c.getCardName() != null && c.getCardName().toLowerCase().contains(name.toLowerCase())) ||
                        (c.getCardSetId() != null && c.getCardSetId().toLowerCase().contains(name.toLowerCase())))
                .filter(c -> type == null || type.name().equalsIgnoreCase(c.getCardType()))
                .filter(c -> colors == null || colors.isEmpty() || matchesAnyColor(c.getCardColor(), colors))
                .filter(c -> rarities == null || rarities.isEmpty() ||
                        rarities.stream().anyMatch(r -> r.name().equalsIgnoreCase(c.getRarity())))
                .filter(c -> cost == null || cost.equals(parseIntSafe(c.getCardCost())))
                .filter(c -> power == null || power.equals(parseIntSafe(c.getCardPower())))
                .filter(c -> setId == null || (c.getSetId() != null && c.getSetId().equalsIgnoreCase(setId)))
                .filter(c -> counterAmount == null || counterAmount.equals(c.getCounterAmount()))
                .filter(c -> attribute == null || (c.getAttribute() != null && c.getAttribute().equalsIgnoreCase(attribute)))
                .filter(c -> subTypes == null || containsToken(c.getSubTypes(), subTypes))
                .toList();
    }

    private static boolean matchesAnyColor(String cardColor, List<CardColor> colors) {
        if (cardColor == null) {
            return false;
        }
        return Arrays.stream(cardColor.split("\\s+"))
                .anyMatch(token -> colors.stream().anyMatch(c -> c.name().equalsIgnoreCase(token)));
    }

    private static boolean containsToken(String tokens, String value) {
        if (tokens == null) {
            return false;
        }
        return Arrays.stream(tokens.split("\\s+"))
                .anyMatch(token -> token.equalsIgnoreCase(value));
    }

    private static Integer parseIntSafe(String s) {
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

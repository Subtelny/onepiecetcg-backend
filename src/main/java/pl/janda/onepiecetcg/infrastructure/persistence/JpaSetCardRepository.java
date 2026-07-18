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
            List<CardType> types,
            List<CardColor> colors,
            List<CardRarity> rarities,
            Integer cost,
            Integer power,
            List<String> setIds,
            Integer counterAmount,
            List<String> attributes,
            String subTypes,
            List<String> prefixes
    ) {
        return findAll().stream()
                .filter(c -> name == null ||
                        (c.getCardName() != null && c.getCardName().toLowerCase().contains(name.toLowerCase())) ||
                        (c.getCardSetId() != null && c.getCardSetId().toLowerCase().contains(name.toLowerCase())))
                .filter(c -> types == null || types.isEmpty() ||
                        types.stream().anyMatch(t -> t.name().equalsIgnoreCase(c.getCardType())))
                .filter(c -> colors == null || colors.isEmpty() || matchesAnyColor(c.getCardColor(), colors))
                .filter(c -> rarities == null || rarities.isEmpty() ||
                        rarities.stream().anyMatch(r -> r.name().equalsIgnoreCase(c.getRarity())))
                .filter(c -> cost == null || cost.equals(parseIntSafe(c.getCardCost())))
                .filter(c -> power == null || power.equals(parseIntSafe(c.getCardPower())))
                .filter(c -> setIds == null || setIds.isEmpty() ||
                        (c.getSetId() != null && setIds.stream().anyMatch(s -> s.equalsIgnoreCase(c.getSetId()))))
                .filter(c -> counterAmount == null || counterAmount.equals(c.getCounterAmount()))
                .filter(c -> attributes == null || attributes.isEmpty() ||
                        (c.getAttribute() != null && attributes.stream().anyMatch(a -> a.equalsIgnoreCase(c.getAttribute()))))
                .filter(c -> subTypes == null || containsToken(c.getSubTypes(), subTypes))
                .filter(c -> prefixes == null || prefixes.isEmpty() ||
                        (c.getCardPrefix() != null && prefixes.stream().anyMatch(p -> p.equalsIgnoreCase(c.getCardPrefix()))))
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

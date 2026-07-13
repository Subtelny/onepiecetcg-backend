package pl.janda.onepiecetcg.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.application.model.Card;
import pl.janda.onepiecetcg.application.model.CardColor;
import pl.janda.onepiecetcg.application.model.CardRarity;
import pl.janda.onepiecetcg.application.model.CardType;
import pl.janda.onepiecetcg.application.repository.CardRepository;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryCardRepository implements CardRepository {

    private final ConcurrentHashMap<String, Card> cards = new ConcurrentHashMap<>();

    @Override
    public List<Card> findAll() {
        return List.copyOf(cards.values());
    }

    @Override
    public Optional<Card> findById(String id) {
        return Optional.ofNullable(cards.get(id));
    }

    @Override
    public Optional<Card> findByCardNumber(String cardNumber) {
        return cards.values().stream()
                .filter(card -> card.getCardNumber().equals(cardNumber))
                .findFirst();
    }

    @Override
    public List<Card> search(
            String name,
            CardType type,
            List<CardColor> colors,
            List<CardRarity> rarities,
            Integer costMin,
            Integer costMax,
            Integer powerMin,
            Integer powerMax
    ) {
        return cards.values().stream()
                .filter(card -> name == null ||
                        card.getName().toLowerCase().contains(name.toLowerCase()) ||
                        card.getCardNumber().toLowerCase().contains(name.toLowerCase()))
                .filter(card -> type == null || card.getType().equals(type))
                .filter(card -> colors == null || colors.isEmpty() ||
                        card.getColor().stream().anyMatch(colors::contains))
                .filter(card -> rarities == null || rarities.isEmpty() ||
                        rarities.contains(card.getRarity()))
                .filter(card -> costMin == null ||
                        (card.getCost() != null && card.getCost() >= costMin))
                .filter(card -> costMax == null ||
                        (card.getCost() != null && card.getCost() <= costMax))
                .filter(card -> powerMin == null ||
                        (card.getPower() != null && card.getPower() >= powerMin))
                .filter(card -> powerMax == null ||
                        (card.getPower() != null && card.getPower() <= powerMax))
                .collect(Collectors.toList());
    }

    // Public method for mock data loading
    public void addCard(Card card) {
        cards.put(card.getId(), card);
    }

    public void clear() {
        cards.clear();
    }
}

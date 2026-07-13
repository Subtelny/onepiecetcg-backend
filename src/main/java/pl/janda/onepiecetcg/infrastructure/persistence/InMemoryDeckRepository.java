package pl.janda.onepiecetcg.infrastructure.persistence;

import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.application.model.Deck;
import pl.janda.onepiecetcg.application.repository.DeckRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Repository
public class InMemoryDeckRepository implements DeckRepository {

    private final ConcurrentHashMap<String, Deck> decks = new ConcurrentHashMap<>();

    @Override
    public List<Deck> findAll() {
        return List.copyOf(decks.values());
    }

    @Override
    public Optional<Deck> findById(String id) {
        return Optional.ofNullable(decks.get(id));
    }

    @Override
    public Deck save(Deck deck) {
        if (deck.getId() == null || deck.getId().isEmpty()) {
            deck.setId(UUID.randomUUID().toString());
        }
        if (deck.getCreatedAt() == null) {
            deck.setCreatedAt(LocalDateTime.now());
        }
        deck.setUpdatedAt(LocalDateTime.now());

        decks.put(deck.getId(), deck);
        return deck;
    }

    @Override
    public Deck update(Deck deck) {
        if (!decks.containsKey(deck.getId())) {
            throw new IllegalArgumentException("Deck not found with id: " + deck.getId());
        }
        deck.setUpdatedAt(LocalDateTime.now());
        decks.put(deck.getId(), deck);
        return deck;
    }

    @Override
    public void deleteById(String id) {
        decks.remove(id);
    }

    @Override
    public List<Deck> search(String name, String color, String leader) {
        return decks.values().stream()
                .filter(deck -> name == null ||
                        deck.getName().toLowerCase().contains(name.toLowerCase()))
                .filter(deck -> color == null ||
                        deck.getLeader().getColor().stream()
                                .anyMatch(c -> c.name().equalsIgnoreCase(color)))
                .filter(deck -> leader == null ||
                        deck.getLeader().getName().toLowerCase().contains(leader.toLowerCase()))
                .collect(Collectors.toList());
    }

    // Public method for mock data loading
    public void addDeck(Deck deck) {
        decks.put(deck.getId(), deck);
    }

    public void clear() {
        decks.clear();
    }
}

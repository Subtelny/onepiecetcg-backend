package pl.janda.onepiecetcg.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.application.model.Deck;
import pl.janda.onepiecetcg.application.repository.DeckRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeckService {

    private final DeckRepository deckRepository;

    public List<Deck> getAllDecks() {
        return deckRepository.findAll();
    }

    public Deck getDeckById(String id) {
        return deckRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Deck not found with id: " + id));
    }

    public List<Deck> searchDecks(String name, String color, String leader) {
        return deckRepository.search(name, color, leader);
    }

    public Deck createDeck(Deck deck) {
        return deckRepository.save(deck);
    }

    public Deck updateDeck(String id, Deck deck) {
        // Verify deck exists
        deckRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Deck not found with id: " + id));

        deck.setId(id);
        return deckRepository.update(deck);
    }

    public void deleteDeck(String id) {
        // Verify deck exists
        deckRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Deck not found with id: " + id));

        deckRepository.deleteById(id);
    }
}

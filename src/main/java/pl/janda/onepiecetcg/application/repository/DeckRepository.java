package pl.janda.onepiecetcg.application.repository;

import pl.janda.onepiecetcg.application.model.Deck;

import java.util.List;
import java.util.Optional;

public interface DeckRepository {

    List<Deck> findAll();

    Optional<Deck> findById(String id);

    Deck save(Deck deck);

    Deck update(Deck deck);

    void deleteById(String id);

    List<Deck> search(String name, String color, String leader);
}

package pl.janda.onepiecetcg.infrastructure.persistence;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import pl.janda.onepiecetcg.application.model.CardColor;
import pl.janda.onepiecetcg.application.model.CardRarity;
import pl.janda.onepiecetcg.application.model.CardSearchField;
import pl.janda.onepiecetcg.application.model.CardSortField;
import pl.janda.onepiecetcg.application.model.CardType;
import pl.janda.onepiecetcg.application.model.SetCard;
import pl.janda.onepiecetcg.application.model.SortDirection;
import pl.janda.onepiecetcg.application.repository.SetCardRepository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class JpaSetCardRepository implements SetCardRepository {

    private final SetCardJpaRepository jpaRepository;

    private final JooqSetCardQueryAdapter jooqQueryAdapter;

    @Override
    public List<SetCard> findAll() {
        return jpaRepository.findAll();
    }

    @Override
    public void deleteAll() {
        jpaRepository.deleteAll();
    }

    @Override
    public <S extends SetCard> List<S> saveAll(Iterable<S> setCards) {
        // Flush is required here: recomputeRepresentative()/search() run raw JOOQ SQL against the same
        // transaction's connection, bypassing the Hibernate session, so pending writes (including the
        // deleteAll() that normally precedes this call) must be physically written first or those
        // JOOQ queries would see stale/incomplete data.
        return jpaRepository.saveAllAndFlush(setCards);
    }

    @Override
    public Optional<SetCard> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public List<SetCard> findByCardSetId(String cardSetId) {
        return jpaRepository.findByCardSetId(cardSetId);
    }

    @Override
    public void recomputeRepresentative() {
        jooqQueryAdapter.recomputeRepresentative();
    }

    @Override
    public List<SetCard> search(
            String name,
            CardSearchField searchField,
            List<CardType> types,
            List<CardColor> colors,
            List<CardRarity> rarities,
            List<CardRarity> flatRarities,
            List<Integer> costs,
            Integer power,
            Integer counterAmount,
            List<String> attributes,
            List<String> attributeCombos,
            String subTypes,
            List<String> prefixes,
            CardSortField sortBy,
            SortDirection sortOrder,
            int page,
            int limit
    ) {
        return jooqQueryAdapter.search(name, searchField, types, colors, rarities, flatRarities, costs, power, counterAmount,
                attributes, attributeCombos, subTypes, prefixes, sortBy, sortOrder, page, limit);
    }

    @Override
    public long countSearch(
            String name,
            CardSearchField searchField,
            List<CardType> types,
            List<CardColor> colors,
            List<CardRarity> rarities,
            List<CardRarity> flatRarities,
            List<Integer> costs,
            Integer power,
            Integer counterAmount,
            List<String> attributes,
            List<String> attributeCombos,
            String subTypes,
            List<String> prefixes
    ) {
        return jooqQueryAdapter.countSearch(name, searchField, types, colors, rarities, flatRarities, costs, power, counterAmount,
                attributes, attributeCombos, subTypes, prefixes);
    }
}

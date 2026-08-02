package pl.janda.onepiecetcg.cards.application.repository;

import pl.janda.onepiecetcg.cards.application.model.OnePieceCardSet;

import java.util.List;

public interface OnePieceCardSetRepository {

    List<OnePieceCardSet> findAll();
}

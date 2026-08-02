package pl.janda.onepiecetcg.cards.application.repository;

import pl.janda.onepiecetcg.cards.application.model.OnePieceCard;

import java.util.List;

public interface OnePieceCardRepository {

    List<OnePieceCard> findAll();
}

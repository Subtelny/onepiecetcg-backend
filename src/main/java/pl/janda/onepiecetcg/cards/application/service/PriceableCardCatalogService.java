package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.cards.application.model.PriceableCard;
import pl.janda.onepiecetcg.cards.application.port.in.PriceableCardCatalogUseCase;
import pl.janda.onepiecetcg.cards.application.repository.SetCardQueryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceableCardCatalogService implements PriceableCardCatalogUseCase {

    private final SetCardQueryRepository setCardRepository;

    @Override
    public List<PriceableCard> getPriceableCards() {
        return setCardRepository.findAllPriceableCards();
    }
}

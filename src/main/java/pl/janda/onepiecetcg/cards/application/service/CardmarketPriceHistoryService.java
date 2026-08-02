package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.janda.onepiecetcg.cards.application.model.CardmarketPriceCandidate;
import pl.janda.onepiecetcg.cards.application.repository.CardmarketPriceCandidateRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardmarketPriceHistoryService {

    private final CardmarketPriceCandidateRepository cardmarketPriceCandidateRepository;

    @Transactional
    public void appendAll(List<CardmarketPriceCandidate> candidates) {
        cardmarketPriceCandidateRepository.saveAll(candidates);
    }
}

package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.janda.onepiecetcg.cards.application.model.CardFaq;
import pl.janda.onepiecetcg.cards.application.repository.CardFaqRepository;

import java.util.List;


@Service
@RequiredArgsConstructor
public class CardFaqReplacementService {

    private final CardFaqRepository cardFaqRepository;

    @Transactional
    public void replaceSet(String setId, List<CardFaq> faqEntries) {
        cardFaqRepository.deleteBySetId(setId);
        cardFaqRepository.saveAll(faqEntries);
    }
}

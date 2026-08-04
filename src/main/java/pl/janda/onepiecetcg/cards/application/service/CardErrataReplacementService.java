package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pl.janda.onepiecetcg.cards.application.model.CardErrata;
import pl.janda.onepiecetcg.cards.application.repository.CardErrataRepository;

import java.util.List;


@Service
@RequiredArgsConstructor
public class CardErrataReplacementService {

    private final CardErrataRepository cardErrataRepository;

    @Transactional
    public void replaceAll(List<CardErrata> errata) {
        cardErrataRepository.deleteAll();
        cardErrataRepository.saveAll(errata);
    }
}

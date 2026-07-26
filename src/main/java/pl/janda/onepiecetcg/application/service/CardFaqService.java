package pl.janda.onepiecetcg.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.application.model.CardFaq;
import pl.janda.onepiecetcg.application.repository.CardFaqRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardFaqService {

    private final CardFaqRepository cardFaqRepository;

    /**
     * Resolves the full FAQ history per card code (a card can accumulate more than one Q&A entry
     * over time), sorted oldest to newest. Missing/never-clarified codes are simply absent from the
     * returned map.
     */
    public Map<String, List<CardFaq>> historyByCardCodes(List<String> cardCodes) {
        var distinctCodes = cardCodes.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctCodes.isEmpty()) {
            return Map.of();
        }
        return cardFaqRepository.findByCardCodeIn(distinctCodes).stream()
                .collect(Collectors.groupingBy(
                        CardFaq::getCardCode,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .sorted(Comparator.comparing(CardFaq::getPublishedDate))
                                        .toList())));
    }
}

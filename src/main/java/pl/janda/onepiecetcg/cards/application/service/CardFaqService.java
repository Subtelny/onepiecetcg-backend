package pl.janda.onepiecetcg.cards.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.cards.application.model.CardFaq;
import pl.janda.onepiecetcg.cards.application.port.in.CardFaqQueryUseCase;
import pl.janda.onepiecetcg.cards.application.repository.CardFaqRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardFaqService implements CardFaqQueryUseCase {

    private final CardFaqRepository cardFaqRepository;


    @Override
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

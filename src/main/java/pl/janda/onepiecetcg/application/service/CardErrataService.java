package pl.janda.onepiecetcg.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.application.model.CardErrata;
import pl.janda.onepiecetcg.application.repository.CardErrataRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CardErrataService {

    private final CardErrataRepository cardErrataRepository;

    public List<CardErrata> listAll() {
        return cardErrataRepository.findAll();
    }

    /**
     * Resolves the full errata history per card code (a card can receive more than one errata over
     * time), sorted oldest to newest. Missing/never-erratad codes are simply absent from the
     * returned map.
     */
    public Map<String, List<CardErrata>> historyByCardCodes(List<String> cardCodes) {
        var distinctCodes = cardCodes.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctCodes.isEmpty()) {
            return Map.of();
        }
        return cardErrataRepository.findByCardCodeIn(distinctCodes).stream()
                .collect(Collectors.groupingBy(
                        CardErrata::getCardCode,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .sorted(Comparator.comparing(CardErrata::getNoticeDate))
                                        .toList())));
    }
}

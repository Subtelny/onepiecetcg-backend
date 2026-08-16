package pl.janda.onepiecetcg.pricing.application.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pl.janda.onepiecetcg.pricing.application.model.PriceHistoryPoint;
import pl.janda.onepiecetcg.pricing.application.model.PriceQuote;
import pl.janda.onepiecetcg.pricing.application.port.in.PriceQueryUseCase;
import pl.janda.onepiecetcg.pricing.application.repository.PriceHistoryRepository;
import pl.janda.onepiecetcg.pricing.application.repository.PriceQuoteRepository;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PriceQueryService implements PriceQueryUseCase {

    private final PriceQuoteRepository priceQuoteRepository;

    private final PriceHistoryRepository priceHistoryRepository;

    @Override
    public Map<String, List<PriceQuote>> getLatestPricesByReferences(List<String> priceReferences) {
        if (priceReferences == null || priceReferences.isEmpty()) {
            return Map.of();
        }
        var distinctReferences = priceReferences.stream()
                .filter(Objects::nonNull)
                .filter(reference -> !reference.isBlank())
                .distinct()
                .toList();
        if (distinctReferences.isEmpty()) {
            return Map.of();
        }
        return priceQuoteRepository.findLatestByPriceReferences(distinctReferences).stream()
                .collect(Collectors.groupingBy(PriceQuote::getPriceReference));
    }

    @Override
    public List<PriceHistoryPoint> getPriceHistoryByReference(String priceReference) {
        if (priceReference == null || priceReference.isBlank()) {
            return List.of();
        }
        return priceHistoryRepository.findHistoryByPriceReference(priceReference);
    }
}

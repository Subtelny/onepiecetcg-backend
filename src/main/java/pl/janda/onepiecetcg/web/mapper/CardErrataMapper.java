package pl.janda.onepiecetcg.web.mapper;

import org.springframework.stereotype.Component;
import pl.janda.onepiecetcg.application.model.CardErrata;
import pl.janda.onepiecetcg.web.dto.CardErrataDto;

import java.util.List;

@Component
public class CardErrataMapper {

    public CardErrataDto toDto(CardErrata errata) {
        if (errata == null) {
            return null;
        }

        return CardErrataDto.builder()
                .cardCode(errata.getCardCode())
                .cardName(errata.getCardName())
                .scopeNote(errata.getScopeNote())
                .beforeText(errata.getBeforeText())
                .afterText(errata.getAfterText())
                .noticeDate(errata.getNoticeDate() != null ? errata.getNoticeDate().toString() : null)
                .sourceUrl(errata.getSourceUrl())
                .build();
    }

    public List<CardErrataDto> toDtoList(List<CardErrata> errata) {
        return errata != null ?
                errata.stream().map(this::toDto).toList() : List.of();
    }
}

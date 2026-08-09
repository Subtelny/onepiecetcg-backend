package pl.janda.onepiecetcg.matchups.application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawMatchupSnapshot {

    private Long id;

    private String dataset;

    private Long totalMatches;

    private OffsetDateTime scrapedAt;
}

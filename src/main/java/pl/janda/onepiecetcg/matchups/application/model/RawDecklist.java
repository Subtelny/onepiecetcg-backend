package pl.janda.onepiecetcg.matchups.application.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RawDecklist {

    private Long snapshotId;

    private String leader;

    private String deck;

    private Long games;
}

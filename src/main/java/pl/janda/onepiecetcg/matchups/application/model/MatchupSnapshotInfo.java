package pl.janda.onepiecetcg.matchups.application.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@Entity
@Table(name = "matchup_snapshot_info")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchupSnapshotInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_snapshot_id")
    private Long sourceSnapshotId;

    @Column(name = "dataset", nullable = false)
    private String dataset;

    @Column(name = "total_matches", nullable = false)
    private Long totalMatches;

    @Column(name = "scraped_at", nullable = false)
    private OffsetDateTime scrapedAt;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @Column(name = "card_profile_version")
    private Integer cardProfileVersion;
}

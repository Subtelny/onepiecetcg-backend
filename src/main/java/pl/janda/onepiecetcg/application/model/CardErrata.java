package pl.janda.onepiecetcg.application.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "card_errata")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardErrata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_code", nullable = false)
    private String cardCode;

    @Column(name = "card_name")
    private String cardName;

    @Column(name = "scope_note")
    private String scopeNote;

    @Column(name = "before_text", columnDefinition = "TEXT")
    private String beforeText;

    @Column(name = "after_text", columnDefinition = "TEXT")
    private String afterText;

    @Column(name = "notice_date", nullable = false)
    private LocalDate noticeDate;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;
}

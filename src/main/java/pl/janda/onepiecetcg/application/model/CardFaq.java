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
@Table(name = "card_faq")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CardFaq {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_code", nullable = false)
    private String cardCode;

    @Column(name = "card_name")
    private String cardName;

    @Column(name = "set_id", nullable = false)
    private String setId;

    @Column(name = "question", columnDefinition = "TEXT", nullable = false)
    private String question;

    @Column(name = "answer", columnDefinition = "TEXT", nullable = false)
    private String answer;

    @Column(name = "published_date", nullable = false)
    private LocalDate publishedDate;

    @Column(name = "source_url")
    private String sourceUrl;

    @Column(name = "last_synced_at")
    private LocalDateTime lastSyncedAt;
}

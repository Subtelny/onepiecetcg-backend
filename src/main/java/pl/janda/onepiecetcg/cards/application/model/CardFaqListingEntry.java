package pl.janda.onepiecetcg.cards.application.model;

import java.time.LocalDate;

public record CardFaqListingEntry(String setId, LocalDate publishedDate, String pdfUrl) {
}

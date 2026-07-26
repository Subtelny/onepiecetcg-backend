package pl.janda.onepiecetcg.application.model;

import java.time.LocalDate;

public record CardFaqListingEntry(String setId, LocalDate publishedDate, String pdfUrl) {
}

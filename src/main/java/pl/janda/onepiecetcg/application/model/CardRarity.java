package pl.janda.onepiecetcg.application.model;

public enum CardRarity {
    C,    // Common
    UC,   // Uncommon
    R,    // Rare
    SR,   // Super Rare
    L,    // Leader
    SEC,  // Secret Rare
    TR,   // Treasure Rare
    PR,   // Promo - always the lowest-priority rarity for canonical-variant ranking (CardService.rarityRank)
    P     // flatRarity override for set cards whose cardPrefix is "P" (SetCardSyncService)
}

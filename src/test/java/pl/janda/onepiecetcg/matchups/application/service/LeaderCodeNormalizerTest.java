package pl.janda.onepiecetcg.matchups.application.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LeaderCodeNormalizerTest {

    private final LeaderCodeNormalizer normalizer = new LeaderCodeNormalizer();

    @Test
    void extractCardCode_parsesCleanCountPrefixedShape() {
        assertThat(normalizer.extractCardCode("1xOP14-020")).contains("OP14-020");
    }

    @Test
    void extractCardCode_parsesSpaceSeparatedShapeWithTrailingCardName() {
        assertThat(normalizer.extractCardCode("1 OP13-079 Imu")).contains("OP13-079");
    }

    @Test
    void extractCardCode_parsesNonLeaderCardCodeShapeGenerically() {
        assertThat(normalizer.extractCardCode("4xST34-003")).contains("ST34-003");
    }

    @Test
    void extractCardCode_returnsEmptyForUnparseableGarbage() {
        assertThat(normalizer.extractCardCode("not a card code")).isEmpty();
    }

    @Test
    void extractCardCode_returnsEmptyForBlankOrNullInput() {
        assertThat(normalizer.extractCardCode(null)).isEmpty();
        assertThat(normalizer.extractCardCode("  ")).isEmpty();
    }
}

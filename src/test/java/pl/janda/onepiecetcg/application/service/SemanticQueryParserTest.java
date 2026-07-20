package pl.janda.onepiecetcg.application.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SemanticQueryParserTest {

    private final SemanticQueryParser parser = new SemanticQueryParser();

    @Test
    void parse_costToken_extractsCostAsIs() {
        var result = parser.parse("6c");

        assertThat(result.cost()).isEqualTo(6);
        assertThat(result.counter()).isNull();
        assertThat(result.power()).isNull();
        assertThat(result.remainingText()).isEmpty();
    }

    @Test
    void parse_counterToken_extractsCounterTimesOneThousand() {
        var result = parser.parse("2kc");

        assertThat(result.counter()).isEqualTo(2000);
        assertThat(result.cost()).isNull();
        assertThat(result.power()).isNull();
        assertThat(result.remainingText()).isEmpty();
    }

    @Test
    void parse_powerToken_extractsPowerTimesOneThousand() {
        var result = parser.parse("5kp");

        assertThat(result.power()).isEqualTo(5000);
        assertThat(result.cost()).isNull();
        assertThat(result.counter()).isNull();
        assertThat(result.remainingText()).isEmpty();
    }

    @Test
    void parse_mixedTokensWithFreeText_extractsAllTokensAndKeepsRemainingText() {
        var result = parser.parse("rush 6c 2kc");

        assertThat(result.cost()).isEqualTo(6);
        assertThat(result.counter()).isEqualTo(2000);
        assertThat(result.power()).isNull();
        assertThat(result.remainingText()).isEqualTo("rush");
    }

    @Test
    void parse_noTokens_leavesTextUntouched() {
        var result = parser.parse("straw hat crew");

        assertThat(result.cost()).isNull();
        assertThat(result.counter()).isNull();
        assertThat(result.power()).isNull();
        assertThat(result.remainingText()).isEqualTo("straw hat crew");
    }

    @Test
    void parse_isCaseInsensitiveForSuffixes() {
        var result = parser.parse("6C 2KC 5KP");

        assertThat(result.cost()).isEqualTo(6);
        assertThat(result.counter()).isEqualTo(2000);
        assertThat(result.power()).isEqualTo(5000);
        assertThat(result.remainingText()).isEmpty();
    }

    @Test
    void parse_tokenEmbeddedMidWord_isNotTreatedAsAToken() {
        // No word boundary before the digits (attached to "abc") or after the suffix (attached to
        // "ats") - must not be parsed as a cost token.
        var prefixed = parser.parse("abc6c");
        assertThat(prefixed.cost()).isNull();
        assertThat(prefixed.remainingText()).isEqualTo("abc6c");

        var suffixed = parser.parse("6cats");
        assertThat(suffixed.cost()).isNull();
        assertThat(suffixed.remainingText()).isEqualTo("6cats");
    }

    @Test
    void parse_collapsesWhitespaceLeftBehindByRemovedTokens() {
        var result = parser.parse("rush  6c  now");

        assertThat(result.cost()).isEqualTo(6);
        assertThat(result.remainingText()).isEqualTo("rush now");
    }

    @Test
    void parse_nullQuery_returnsEmptyResult() {
        var result = parser.parse(null);

        assertThat(result.remainingText()).isEmpty();
        assertThat(result.cost()).isNull();
        assertThat(result.counter()).isNull();
        assertThat(result.power()).isNull();
    }
}

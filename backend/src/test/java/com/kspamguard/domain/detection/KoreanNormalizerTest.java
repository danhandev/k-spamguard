package com.kspamguard.domain.detection;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KoreanNormalizerTest {

    private final KoreanNormalizer normalizer = new KoreanNormalizer();

    @Test
    void reduceJamoRepetition() {
        assertThat(normalizer.normalize("ㅋㅋㅋㅋㅋ")).isEqualTo("ㅋㅋ");
    }

    @Test
    void reduceSyllableRepetition() {
        assertThat(normalizer.normalize("좋아아아아")).isEqualTo("좋아아");
    }

    @Test
    void collapseWhitespace() {
        assertThat(normalizer.normalize("안녕   하세요")).isEqualTo("안녕 하세요");
    }

    @Test
    void trimLeadingAndTrailingSpaces() {
        assertThat(normalizer.normalize("  hello  ")).isEqualTo("hello");
    }

    @Test
    void lowercaseEnglish() {
        assertThat(normalizer.normalize("SPAM")).isEqualTo("spam");
    }

    @Test
    void nullReturnsEmpty() {
        assertThat(normalizer.normalize(null)).isEmpty();
    }

    @Test
    void emptyStringReturnsEmpty() {
        assertThat(normalizer.normalize("")).isEmpty();
    }
}

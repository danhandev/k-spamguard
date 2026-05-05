package com.kspamguard.domain.detection;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KoreanTextNormalizerTest {

    private final KoreanTextNormalizer normalizer = new KoreanTextNormalizer();

    // ── NFKC ──────────────────────────────────────────────────────────────────

    @Test
    void nfkc_fullWidthAsciiLetter_normalized() {
        // Ａ (U+FF21) → NFKC → A → lowercase → a
        assertThat(normalizer.normalize("Ａ카지노")).isEqualTo("a카지노");
    }

    // ── Lowercase ─────────────────────────────────────────────────────────────

    @Test
    void lowercase_englishUppercase_converted() {
        assertThat(normalizer.normalize("SPAM")).isEqualTo("spam");
    }

    // ── Leet substitution ─────────────────────────────────────────────────────

    @Test
    void leet_zeroAdjacentToLetter_convertedToO() {
        assertThat(normalizer.normalize("0nlyfans")).isEqualTo("onlyfans");
    }

    @Test
    void leet_oneAdjacentToLetter_convertedToI() {
        assertThat(normalizer.normalize("fr1end")).isEqualTo("friend");
    }

    @Test
    void leet_standaloneDigitSequence_notConverted() {
        // 010-1234-5678: digits not adjacent to letters → unchanged
        assertThat(normalizer.normalize("010-1234-5678")).isEqualTo("010-1234-5678");
    }

    // ── Special chars between Korean ──────────────────────────────────────────

    @Test
    void specialChar_middleDotBetweenKorean_removed() {
        assertThat(normalizer.normalize("카·지·노")).isEqualTo("카지노");
    }

    @Test
    void specialChar_dotBetweenKorean_removed() {
        assertThat(normalizer.normalize("무.료.쿠.폰")).isEqualTo("무료쿠폰");
    }

    @Test
    void specialChar_dashBetweenKorean_removed() {
        assertThat(normalizer.normalize("카-지-노")).isEqualTo("카지노");
    }

    // ── Korean space obfuscation ──────────────────────────────────────────────

    @Test
    void spaceObfuscation_fourSingleKoreanChars_merged() {
        assertThat(normalizer.normalize("무 료 쿠 폰")).isEqualTo("무료쿠폰");
    }

    @Test
    void spaceObfuscation_threeSingleKoreanChars_merged() {
        assertThat(normalizer.normalize("카 지 노")).isEqualTo("카지노");
    }

    @Test
    void spaceObfuscation_twoSingleKoreanChars_notMerged() {
        // 2자 연속은 obfuscation으로 보지 않음 (최소 3자 연속에만 적용)
        assertThat(normalizer.normalize("무 료")).isEqualTo("무 료");
    }

    @Test
    void spaceObfuscation_twoCharKoreanWords_spacesPreserved() {
        // "공구"(2자), "링크"(2자) — 단어 내 공백 없음, 단어 간 공백 보존
        assertThat(normalizer.normalize("공구 링크")).isEqualTo("공구 링크");
    }

    // ── Whitespace ────────────────────────────────────────────────────────────

    @Test
    void whitespace_multipleSpaces_collapsed() {
        assertThat(normalizer.normalize("안녕   하세요")).isEqualTo("안녕 하세요");
    }

    @Test
    void whitespace_trimLeadingAndTrailing() {
        assertThat(normalizer.normalize("  hello  ")).isEqualTo("hello");
    }

    // ── Repetition reduction ──────────────────────────────────────────────────

    @Test
    void repetition_syllableRun_reducedToTwo() {
        assertThat(normalizer.normalize("좋아아아아")).isEqualTo("좋아아");
    }

    @Test
    void repetition_jamoRun_reducedToTwo() {
        // NFKC가 ㅋ(U+314B)을 Hangul Jamo(U+110F)로 변환하므로 정확한 코드포인트 대신 길이만 검증
        assertThat(normalizer.normalize("ㅋㅋㅋㅋ")).hasSize(2);
    }

    // ── Edge cases ────────────────────────────────────────────────────────────

    @Test
    void null_returnsEmpty() {
        assertThat(normalizer.normalize(null)).isEmpty();
    }

    @Test
    void empty_returnsEmpty() {
        assertThat(normalizer.normalize("")).isEmpty();
    }

    @Test
    void blank_returnsEmpty() {
        assertThat(normalizer.normalize("   ")).isEmpty();
    }
}

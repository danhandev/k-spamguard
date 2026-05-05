package com.kspamguard.domain.detection;

import com.kspamguard.domain.rule.SpamRule;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedSpamDetectorTest {

    private final SpamDetector detector = new RuleBasedSpamDetector(new KoreanNormalizer());

    @Test
    void keywordMatch_highScore_returnsSPAM() {
        var rules = List.of(SpamRule.keyword("r1", "맞팔", 0.8));
        var result = detector.detect(new Comment("맞팔 해요 소통해요"), rules);

        assertThat(result.verdict()).isEqualTo(Verdict.SPAM);
        assertThat(result.score()).isEqualTo(0.8);
        assertThat(result.matchedRules()).hasSize(1);
    }

    @Test
    void regexMatch_midScore_returnsSUSPECT() {
        var rules = List.of(SpamRule.regex("r1", "\\d{3}-\\d{4}-\\d{4}", 0.5));
        var result = detector.detect(new Comment("연락처 010-1234-5678"), rules);

        assertThat(result.verdict()).isEqualTo(Verdict.SUSPECT);
        assertThat(result.score()).isEqualTo(0.5);
    }

    @Test
    void noMatch_returnsSAFE() {
        var rules = List.of(SpamRule.keyword("r1", "스팸", 0.9));
        var result = detector.detect(new Comment("오늘 날씨 좋네요"), rules);

        assertThat(result.verdict()).isEqualTo(Verdict.SAFE);
        assertThat(result.score()).isZero();
        assertThat(result.matchedRules()).isEmpty();
    }

    @Test
    void repeatedSyllablesNormalizedBeforeMatch() {
        var rules = List.of(SpamRule.keyword("r1", "좋아", 0.8));
        // "좋아아아아" normalizes to "좋아아" which still contains "좋아"
        var result = detector.detect(new Comment("좋아아아아 정말"), rules);

        assertThat(result.verdict()).isEqualTo(Verdict.SPAM);
    }

    @Test
    void emptyRules_returnsSAFE() {
        var result = detector.detect(new Comment("맞팔 팔로우"), List.of());

        assertThat(result.verdict()).isEqualTo(Verdict.SAFE);
        assertThat(result.score()).isZero();
        assertThat(result.matchedRules()).isEmpty();
    }

    @Test
    void scoreExactlyAt07Boundary_returnsSPAM() {
        var rules = List.of(SpamRule.keyword("r1", "스팸", 0.7));
        var result = detector.detect(new Comment("스팸"), rules);

        assertThat(result.verdict()).isEqualTo(Verdict.SPAM);
    }

    @Test
    void scoreExactlyAt03Boundary_returnsSUSPECT() {
        var rules = List.of(SpamRule.keyword("r1", "홍보", 0.3));
        var result = detector.detect(new Comment("홍보"), rules);

        assertThat(result.verdict()).isEqualTo(Verdict.SUSPECT);
    }

    @Test
    void scoreBelowSuspectThreshold_returnsSAFE() {
        var rules = List.of(SpamRule.keyword("r1", "좋아요", 0.29));
        var result = detector.detect(new Comment("좋아요"), rules);

        assertThat(result.verdict()).isEqualTo(Verdict.SAFE);
    }

    @Test
    void multipleMatches_scoreIsCappedAt1() {
        var rules = List.of(
                SpamRule.keyword("r1", "맞팔", 0.8),
                SpamRule.keyword("r2", "팔로우", 0.8)
        );
        var result = detector.detect(new Comment("맞팔 팔로우"), rules);

        assertThat(result.score()).isEqualTo(1.0);
        assertThat(result.verdict()).isEqualTo(Verdict.SPAM);
        assertThat(result.matchedRules()).hasSize(2);
    }
}

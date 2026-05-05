package com.kspamguard.domain.detection;

import static org.assertj.core.api.Assertions.assertThat;

import com.kspamguard.domain.rule.SpamRule;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpamDetectionEngineTest {

  private final SpamDetector engine = new SpamDetectionEngine(new KoreanTextNormalizer());

  /** 표준 룰셋: 각 테스트는 필요한 룰만 인자로 전달한다. 전체 시나리오 테스트에서는 아래 상수를 공유한다. */
  private static final SpamRule MUKYO_KUPON = SpamRule.keyword("MUKYO_KUPON", "무료쿠폰", 0.9);

  private static final SpamRule CASINO = SpamRule.keyword("CASINO", "카지노", 0.9);
  private static final SpamRule PROF_INCOME = SpamRule.keyword("PROF_INCOME", "수익 인증", 0.4);
  private static final SpamRule DM_CTA = SpamRule.keyword("DM_CTA", "dm", 0.4);
  private static final SpamRule PHONE_NUM =
      SpamRule.regex("PHONE_NUM", "\\d{3}-\\d{4}-\\d{4}", 0.5);

  private static final List<SpamRule> FULL_RULES =
      List.of(MUKYO_KUPON, CASINO, PROF_INCOME, DM_CTA, PHONE_NUM);

  // ── 필수 시나리오 ──────────────────────────────────────────────────────────

  @Test
  void directKeyword_무료쿠폰_returnsSPAM() {
    var result = engine.detect(new Comment("무료쿠폰 받아가세요"), FULL_RULES);
    assertThat(result.status()).isEqualTo(DetectionStatus.SPAM);
    assertThat(result.reasonCodes()).contains("MUKYO_KUPON");
  }

  @Test
  void spaceObfuscation_무_료_쿠_폰_returnsSPAM() {
    var result = engine.detect(new Comment("무 료 쿠 폰 받아가세요"), FULL_RULES);
    assertThat(result.status()).isEqualTo(DetectionStatus.SPAM);
    assertThat(result.reasonCodes()).contains("MUKYO_KUPON");
  }

  @Test
  void dotObfuscation_무료쿠폰_returnsSPAM() {
    var result = engine.detect(new Comment("무.료.쿠.폰 이벤트"), FULL_RULES);
    assertThat(result.status()).isEqualTo(DetectionStatus.SPAM);
    assertThat(result.reasonCodes()).contains("MUKYO_KUPON");
  }

  @Test
  void middleDotObfuscation_카지노_returnsSPAM() {
    var result = engine.detect(new Comment("카·지·노 링크 확인"), FULL_RULES);
    assertThat(result.status()).isEqualTo(DetectionStatus.SPAM);
    assertThat(result.reasonCodes()).contains("CASINO");
  }

  @Test
  void incomeProofAndDm_returnsSPAMOrSUSPECT() {
    var result = engine.detect(new Comment("수익 인증 원하시면 DM"), FULL_RULES);
    assertThat(result.status()).isIn(DetectionStatus.SUSPECT, DetectionStatus.SPAM);
    assertThat(result.reasonCodes()).contains("PROF_INCOME", "DM_CTA");
  }

  // ── 점수 경계 ──────────────────────────────────────────────────────────────

  @Test
  void scoreBoundary_atOrAbove07_returnsSPAM() {
    var rules = List.of(SpamRule.keyword("R1", "스팸", 0.7));
    var result = engine.detect(new Comment("스팸"), rules);
    assertThat(result.status()).isEqualTo(DetectionStatus.SPAM);
    assertThat(result.score()).isEqualTo(0.7);
  }

  @Test
  void scoreBoundary_atOrAbove03_returnsSUSPECT() {
    var rules = List.of(SpamRule.keyword("R1", "홍보", 0.3));
    var result = engine.detect(new Comment("홍보"), rules);
    assertThat(result.status()).isEqualTo(DetectionStatus.SUSPECT);
    assertThat(result.score()).isEqualTo(0.3);
  }

  @Test
  void scoreBoundary_below03_returnsSAFE() {
    var rules = List.of(SpamRule.keyword("R1", "좋아요", 0.29));
    var result = engine.detect(new Comment("좋아요"), rules);
    assertThat(result.status()).isEqualTo(DetectionStatus.SAFE);
  }

  @Test
  void multipleMatches_scoreCappedAt1() {
    var rules = List.of(SpamRule.keyword("R1", "맞팔", 0.8), SpamRule.keyword("R2", "팔로우", 0.8));
    var result = engine.detect(new Comment("맞팔 팔로우"), rules);
    assertThat(result.score()).isEqualTo(1.0);
    assertThat(result.status()).isEqualTo(DetectionStatus.SPAM);
    assertThat(result.matches()).hasSize(2);
  }

  // ── RuleMatch / reasonCodes ────────────────────────────────────────────────

  @Test
  void keywordMatch_ruleMatchContainsMatchedText() {
    var rules = List.of(SpamRule.keyword("R1", "맞팔", 0.8));
    var result = engine.detect(new Comment("맞팔 해요"), rules);
    assertThat(result.matches()).hasSize(1);
    assertThat(result.matches().get(0).matchedText()).isEqualTo("맞팔");
    assertThat(result.matches().get(0).reasonCode()).isEqualTo("R1");
  }

  @Test
  void regexMatch_ruleMatchContainsMatchedText() {
    var rules = List.of(PHONE_NUM);
    var result = engine.detect(new Comment("연락처 010-1234-5678"), rules);
    assertThat(result.matches()).hasSize(1);
    assertThat(result.matches().get(0).matchedText()).isEqualTo("010-1234-5678");
    assertThat(result.matches().get(0).reasonCode()).isEqualTo("PHONE_NUM");
  }

  @Test
  void reasonCodes_distinctPerRule() {
    var rules = List.of(SpamRule.keyword("R1", "무료쿠폰", 0.5), SpamRule.keyword("R2", "카지노", 0.5));
    var result = engine.detect(new Comment("무료쿠폰 카지노"), rules);
    assertThat(result.reasonCodes()).containsExactlyInAnyOrder("R1", "R2");
  }

  @Test
  void noMatch_reasonCodesEmpty_scoreZero() {
    var rules = List.of(SpamRule.keyword("R1", "스팸", 0.9));
    var result = engine.detect(new Comment("오늘 날씨 좋네요"), rules);
    assertThat(result.status()).isEqualTo(DetectionStatus.SAFE);
    assertThat(result.score()).isZero();
    assertThat(result.reasonCodes()).isEmpty();
  }

  @Test
  void emptyRules_returnsSAFE() {
    var result = engine.detect(new Comment("맞팔 팔로우"), List.of());
    assertThat(result.status()).isEqualTo(DetectionStatus.SAFE);
    assertThat(result.score()).isZero();
    assertThat(result.matches()).isEmpty();
  }

  @Test
  void regexMatch_phoneNumber_returnsSUSPECT() {
    var rules = List.of(PHONE_NUM);
    var result = engine.detect(new Comment("연락처 010-9999-1234"), rules);
    assertThat(result.status()).isEqualTo(DetectionStatus.SUSPECT);
    assertThat(result.reasonCodes()).contains("PHONE_NUM");
  }

  // ── 엣지 케이스 ───────────────────────────────────────────────────────────

  @Test
  void nullCommentText_returnsSAFE() {
    var rules = List.of(SpamRule.keyword("R1", "스팸", 0.9));
    var result = engine.detect(new Comment(null), rules);
    assertThat(result.status()).isEqualTo(DetectionStatus.SAFE);
    assertThat(result.matches()).isEmpty();
  }

  @Test
  void invalidRegexPattern_doesNotThrow_treatedAsNoMatch() {
    var rules = List.of(SpamRule.regex("BAD", "[invalid(", 0.9));
    var result = engine.detect(new Comment("스팸"), rules);
    assertThat(result.status()).isEqualTo(DetectionStatus.SAFE);
    assertThat(result.matches()).isEmpty();
  }

  // ── 오탐 방지 (False Positive) ────────────────────────────────────────────

  @Test
  void falsePositive_casualPraise_SAFE() {
    var result = engine.detect(new Comment("오늘 영상 너무 좋아요"), FULL_RULES);
    assertThat(result.status()).isEqualTo(DetectionStatus.SAFE);
  }

  @Test
  void falsePositive_groupBuyInquiry_SAFE() {
    var result = engine.detect(new Comment("공구 링크 어디서 볼 수 있나요?"), FULL_RULES);
    assertThat(result.status()).isEqualTo(DetectionStatus.SAFE);
  }

  @Test
  void falsePositive_casualAgreement_SAFE() {
    var result = engine.detect(new Comment("맞아요 진짜 맛있어요"), FULL_RULES);
    assertThat(result.status()).isEqualTo(DetectionStatus.SAFE);
  }

  @Test
  void falsePositive_greeting_SAFE() {
    var result = engine.detect(new Comment("오늘도 좋은 하루 보내세요"), FULL_RULES);
    assertThat(result.status()).isEqualTo(DetectionStatus.SAFE);
  }

  @Test
  void falsePositive_reviewThanks_SAFE() {
    var result = engine.detect(new Comment("리뷰 감사합니다"), FULL_RULES);
    assertThat(result.status()).isEqualTo(DetectionStatus.SAFE);
  }

  @Test
  void falsePositive_photoCompliment_SAFE() {
    var result = engine.detect(new Comment("사진 예쁘게 나왔네요"), FULL_RULES);
    assertThat(result.status()).isEqualTo(DetectionStatus.SAFE);
  }

  @Test
  void falsePositive_videoCompliment_SAFE() {
    var result = engine.detect(new Comment("영상 잘 봤습니다"), FULL_RULES);
    assertThat(result.status()).isEqualTo(DetectionStatus.SAFE);
  }
}

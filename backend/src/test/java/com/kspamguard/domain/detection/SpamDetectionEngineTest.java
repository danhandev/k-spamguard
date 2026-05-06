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

  // ── 검색 유도형 성인 스팸 ───────────────────────────────────────────────────
  //
  // 패턴: "[X]를 구글에서 검색하면 [성인 콘텐츠] 나옴"
  // 인물명/단어가 바뀌어도 CTA 구조로 탐지한다.

  // SEARCH_CTA: 검색엔진(구글 변형) + 검색 동사가 50자 이내 공존
  private static final SpamRule SEARCH_CTA =
      SpamRule.regex(
          "SEARCH_CTA", "(?:구 ?글|꾸 ?글|[0-9].{0,2}글).{0,50}검.{0,5}색", 0.8);

  private static final SpamRule CHILD_INCIDENT =
      SpamRule.keyword("CHILD_INCIDENT", "이동사건", 0.9);

  private static final SpamRule BROADCAST_ACCIDENT =
      SpamRule.keyword("BROADCAST_ACCIDENT", "방송사고", 0.7);

  private static final SpamRule ADULT_CUP = SpamRule.keyword("ADULT_CUP", "g컵", 0.7);

  private static final List<SpamRule> ADULT_RULES =
      List.of(SEARCH_CTA, CHILD_INCIDENT, BROADCAST_ACCIDENT, ADULT_CUP);

  @Test
  void adultSpam_spaceObfuscatedChildIncident_returnsSPAM() {
    // "이 동 사 건" → 4글자 공백 병합 → "이동사건" → CHILD_INCIDENT
    var result =
        engine.detect(
            new Comment(
                "실제 댓글 데이터를 가져와 볼게 <성 세라 사건> 꾸 글에 입..력하면 ㅇ ㅏ 이 동 사 건 나옴.ㅋㅋ ㄷㄷㄷ 어린애들은 검색하지 마라 ㅡㅡ미드 장난아니더라"),
            ADULT_RULES);
    assertThat(result.status()).isEqualTo(DetectionStatus.SPAM);
    assertThat(result.reasonCodes()).contains("CHILD_INCIDENT");
  }

  @Test
  void adultSpam_digitObfuscatedGoogleSearch_returnsSPAM() {
    // "9..글에 검. 색" — 숫자+점 변형 구글 + 점+공백 삽입 검색
    // SPECIAL_BETWEEN_KOREAN: 숫자/공백 뒤 특수문자는 미제거 → regex가 그대로 매칭
    var result =
        engine.detect(
            new Comment(
                "[성세라 녹음봄] 9..글에 검. 색하고  방문 자물쇠로 잠궈놓고 달려가 지컵ㄹㅈㄷ그거 나옴ㅋㅋ 이거 밤에 문잠그고 몰래 본다더라ㅋㅋ"),
            ADULT_RULES);
    assertThat(result.status()).isEqualTo(DetectionStatus.SPAM);
    assertThat(result.reasonCodes()).contains("SEARCH_CTA");
  }

  @Test
  void adultSpam_spaceObfuscatedGoogleSearchAndBroadcastAccident_returnsSPAM() {
    // "구 글 검 색하면" → 4글자 공백 병합 → "구글검색하면" → SEARCH_CTA
    // "방송사고" → BROADCAST_ACCIDENT
    var result =
        engine.detect(
            new Comment(
                "성세라 G컵<<구 글 검 색하면 VIP 전용 방송사고나옴ㅋ 강호의 도리를 지키기 위해 ㅈㅍ 던져드림 최근에 플랫폼 옮김ㅋ"),
            ADULT_RULES);
    assertThat(result.status()).isEqualTo(DetectionStatus.SPAM);
    assertThat(result.reasonCodes()).containsAnyOf("SEARCH_CTA", "BROADCAST_ACCIDENT");
  }

  @Test
  void adultSpam_dotObfuscatedBroadcastContent_returnsSPAM() {
    // "전..용..방..송..사..고" → 한글 사이 특수문자 제거 → "전용방송사고" → BROADCAST_ACCIDENT
    // "구 글에 ... 검색" → SEARCH_CTA
    var result =
        engine.detect(
            new Comment(
                "구 글에 [성세라 사건] 검색 ㄱㄱ 어린애들은 검색하지마라 ㅡㅡ미드 장단아니더라 인플루언서 VIP 전..용..방..송..사..고나옴ㅋ"),
            ADULT_RULES);
    assertThat(result.status()).isEqualTo(DetectionStatus.SPAM);
    assertThat(result.reasonCodes()).containsAnyOf("SEARCH_CTA", "BROADCAST_ACCIDENT");
  }

  @Test
  void adultSpam_childIncidentAndAdultCup_returnsSPAM() {
    // "이 동 사 건" → "이동사건" → CHILD_INCIDENT
    // "G컵" → lowercase → "g컵" → ADULT_CUP
    var result =
        engine.detect(
            new Comment(
                "<성세라 G컵> 꾸 글에 입..력하면 ㅇ ㅏ 이 동 사 건 나옴.ㅋㅋ ㄷㄷㄷ 어린애들은 검색하지 마라 ㅡㅡ미드 장난아니더라"),
            ADULT_RULES);
    assertThat(result.status()).isEqualTo(DetectionStatus.SPAM);
    assertThat(result.reasonCodes()).containsAnyOf("CHILD_INCIDENT", "ADULT_CUP");
  }

  @Test
  void adultSpam_falsePositive_reactionComment_SAFE() {
    // 스팸 댓글에 반응하는 정상 댓글 — 오탐 없어야 함
    var result = engine.detect(new Comment("아니... 내가 봤던 거네ㅋㅋㅋㅋ"), ADULT_RULES);
    assertThat(result.status()).isEqualTo(DetectionStatus.SAFE);
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

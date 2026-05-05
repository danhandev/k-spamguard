package com.kspamguard.domain.detection;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KoreanTextNormalizer {

  // 3자 이상의 단글자 한글이 공백으로 구분된 패턴: 무 료 쿠 폰, 카 지 노
  private static final Pattern SINGLE_KOREAN_RUN = Pattern.compile("[가-힣](?: [가-힣]){2,}");

  // 한글 사이의 특수문자: 카·지·노, 무.료.쿠.폰
  private static final Pattern SPECIAL_BETWEEN_KOREAN =
      Pattern.compile("(?<=[가-힣])[^가-힣a-z0-9\\s]+(?=[가-힣])");

  // 문자(한글·영문)의 3회 이상 연속 반복 → 2회로 축소. 숫자는 제외(전화번호 등 보존)
  private static final Pattern REPETITION = Pattern.compile("([가-힣ᄀ-ᇿa-z])\\1{2,}");

  public String normalize(String text) {
    if (text == null || text.isBlank()) return "";

    text = Normalizer.normalize(text, Normalizer.Form.NFKC);
    text = text.toLowerCase(Locale.ROOT);
    text = applyLeet(text);
    text = SPECIAL_BETWEEN_KOREAN.matcher(text).replaceAll("");
    text = removeKoreanSpaceObfuscation(text);
    text = text.replaceAll("\\s+", " ").trim();
    text = REPETITION.matcher(text).replaceAll("$1$1");

    return text;
  }

  // 알파벳에 인접한 숫자만 치환 — 전화번호처럼 숫자만 있는 시퀀스는 보존
  private String applyLeet(String text) {
    text = text.replaceAll("0(?=[a-z])|(?<=[a-z])0", "o");
    text = text.replaceAll("1(?=[a-z])|(?<=[a-z])1", "i");
    text = text.replaceAll("3(?=[a-z])|(?<=[a-z])3", "e");
    text = text.replaceAll("4(?=[a-z])|(?<=[a-z])4", "a");
    text = text.replaceAll("5(?=[a-z])|(?<=[a-z])5", "s");
    text = text.replaceAll("7(?=[a-z])|(?<=[a-z])7", "t");
    text = text.replaceAll("@(?=[a-z])|(?<=[a-z])@", "a");
    return text;
  }

  private String removeKoreanSpaceObfuscation(String text) {
    Matcher m = SINGLE_KOREAN_RUN.matcher(text);
    StringBuilder sb = new StringBuilder();
    while (m.find()) {
      m.appendReplacement(sb, m.group().replace(" ", ""));
    }
    m.appendTail(sb);
    return sb.toString();
  }
}

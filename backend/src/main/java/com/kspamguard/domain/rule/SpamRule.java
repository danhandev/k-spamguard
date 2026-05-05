package com.kspamguard.domain.rule;

public record SpamRule(String id, RuleType type, String pattern, double score) {

  public static SpamRule keyword(String id, String keyword, double score) {
    return new SpamRule(id, RuleType.KEYWORD, keyword, score);
  }

  public static SpamRule regex(String id, String pattern, double score) {
    return new SpamRule(id, RuleType.REGEX, pattern, score);
  }
}

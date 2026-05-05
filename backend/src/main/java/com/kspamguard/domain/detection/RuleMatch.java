package com.kspamguard.domain.detection;

import com.kspamguard.domain.rule.SpamRule;

public record RuleMatch(SpamRule rule, String matchedText) {
  public String reasonCode() {
    return rule.id();
  }
}

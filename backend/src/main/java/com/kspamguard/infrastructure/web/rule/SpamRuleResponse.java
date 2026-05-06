package com.kspamguard.infrastructure.web.rule;

import com.kspamguard.application.rule.SpamRuleView;
import java.time.Instant;

public record SpamRuleResponse(
    Long id,
    String ruleCode,
    String ruleType,
    String pattern,
    double threshold,
    boolean enabled,
    Instant createdAt) {

  public static SpamRuleResponse from(SpamRuleView view) {
    return new SpamRuleResponse(
        view.id(),
        view.ruleCode(),
        view.ruleType(),
        view.pattern(),
        view.threshold(),
        view.enabled(),
        view.createdAt());
  }
}

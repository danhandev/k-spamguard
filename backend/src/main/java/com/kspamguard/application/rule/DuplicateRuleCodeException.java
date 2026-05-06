package com.kspamguard.application.rule;

public class DuplicateRuleCodeException extends RuntimeException {
  public DuplicateRuleCodeException(String ruleCode) {
    super("Rule code already exists: " + ruleCode);
  }
}

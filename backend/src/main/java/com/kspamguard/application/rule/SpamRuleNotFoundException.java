package com.kspamguard.application.rule;

public class SpamRuleNotFoundException extends RuntimeException {
  public SpamRuleNotFoundException(Long id) {
    super("Spam rule not found: " + id);
  }
}

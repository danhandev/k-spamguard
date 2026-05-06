package com.kspamguard.application.rule;

public record CreateSpamRuleCommand(
    String ruleCode, String ruleType, String pattern, double threshold) {}

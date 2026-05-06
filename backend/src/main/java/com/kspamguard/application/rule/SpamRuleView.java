package com.kspamguard.application.rule;

import java.time.Instant;

public record SpamRuleView(
    Long id,
    String ruleCode,
    String ruleType,
    String pattern,
    double threshold,
    boolean enabled,
    Instant createdAt) {}

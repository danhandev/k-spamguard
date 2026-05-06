package com.kspamguard.application.rule;

public record UpdateSpamRuleCommand(Long id, String pattern, double threshold, boolean enabled) {}

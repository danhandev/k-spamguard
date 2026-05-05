package com.kspamguard.domain.detection;

import com.kspamguard.domain.rule.SpamRule;

import java.util.List;

public record DetectionResult(Verdict verdict, double score, List<SpamRule> matchedRules) {
    public DetectionResult {
        matchedRules = List.copyOf(matchedRules);
    }
}

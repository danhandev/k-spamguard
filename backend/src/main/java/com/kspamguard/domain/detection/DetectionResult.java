package com.kspamguard.domain.detection;

import java.util.List;

public record DetectionResult(DetectionStatus status, double score, List<RuleMatch> matches) {
    public DetectionResult {
        matches = List.copyOf(matches);
    }

    public List<String> reasonCodes() {
        return matches.stream().map(RuleMatch::reasonCode).distinct().toList();
    }
}

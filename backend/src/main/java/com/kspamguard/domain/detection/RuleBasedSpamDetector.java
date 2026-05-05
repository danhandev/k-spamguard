package com.kspamguard.domain.detection;

import com.kspamguard.domain.rule.SpamRule;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RuleBasedSpamDetector implements SpamDetector {

    private final KoreanNormalizer normalizer;

    public RuleBasedSpamDetector(KoreanNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    @Override
    public DetectionResult detect(Comment comment, List<SpamRule> rules) {
        String normalized = normalizer.normalize(comment.text());

        List<SpamRule> matched = rules.stream()
                .filter(rule -> matches(normalized, rule))
                .collect(Collectors.toList());

        double score = Math.min(
                matched.stream().mapToDouble(SpamRule::score).sum(),
                1.0
        );

        return new DetectionResult(verdictFor(score), score, matched);
    }

    private boolean matches(String text, SpamRule rule) {
        return switch (rule.type()) {
            case KEYWORD -> text.contains(rule.pattern());
            case REGEX -> Pattern.compile(rule.pattern()).matcher(text).find();
        };
    }

    private Verdict verdictFor(double score) {
        if (score >= 0.7) return Verdict.SPAM;
        if (score >= 0.3) return Verdict.SUSPECT;
        return Verdict.SAFE;
    }
}

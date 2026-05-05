package com.kspamguard.domain.detection;

import com.kspamguard.domain.rule.SpamRule;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class SpamDetectionEngine implements SpamDetector {

  private final KoreanTextNormalizer normalizer;
  private final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();

  public SpamDetectionEngine(KoreanTextNormalizer normalizer) {
    this.normalizer = normalizer;
  }

  @Override
  public DetectionResult detect(Comment comment, List<SpamRule> rules) {
    String normalized = normalizer.normalize(comment.text());

    List<RuleMatch> matches =
        rules.stream()
            .map(rule -> tryMatch(normalized, rule))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toList();

    double score = Math.min(matches.stream().mapToDouble(m -> m.rule().score()).sum(), 1.0);

    return new DetectionResult(statusFor(score), score, matches);
  }

  private Optional<RuleMatch> tryMatch(String text, SpamRule rule) {
    return switch (rule.type()) {
      case KEYWORD ->
          text.contains(rule.pattern())
              ? Optional.of(new RuleMatch(rule, rule.pattern()))
              : Optional.empty();
      case REGEX -> {
        Pattern p;
        try {
          p = patternCache.computeIfAbsent(rule.pattern(), Pattern::compile);
        } catch (PatternSyntaxException ignored) {
          yield Optional.empty();
        }
        Matcher m = p.matcher(text);
        yield m.find() ? Optional.of(new RuleMatch(rule, m.group())) : Optional.empty();
      }
    };
  }

  private DetectionStatus statusFor(double score) {
    if (score >= 0.7) return DetectionStatus.SPAM;
    if (score >= 0.3) return DetectionStatus.SUSPECT;
    return DetectionStatus.SAFE;
  }
}

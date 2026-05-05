package com.kspamguard.domain.detection;

import com.kspamguard.domain.rule.SpamRule;
import java.util.List;

public interface SpamDetector {
  DetectionResult detect(Comment comment, List<SpamRule> rules);
}

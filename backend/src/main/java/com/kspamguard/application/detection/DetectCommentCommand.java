package com.kspamguard.application.detection;

import com.kspamguard.domain.rule.SpamRule;
import java.util.List;

public record DetectCommentCommand(String text, List<SpamRule> rules) {
  public DetectCommentCommand {
    rules = List.copyOf(rules);
  }
}

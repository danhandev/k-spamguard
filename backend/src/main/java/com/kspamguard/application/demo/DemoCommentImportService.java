package com.kspamguard.application.demo;

import com.kspamguard.application.detection.DetectCommentCommand;
import com.kspamguard.application.detection.DetectCommentUseCase;
import com.kspamguard.domain.detection.DetectionResult;
import com.kspamguard.domain.rule.SpamRule;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DemoCommentImportService implements DemoCommentImportUseCase {

  private final DetectCommentUseCase detectCommentUseCase;
  private final List<SpamRule> defaultRules;

  public DemoCommentImportService(
      DetectCommentUseCase detectCommentUseCase, List<SpamRule> defaultRules) {
    this.detectCommentUseCase = detectCommentUseCase;
    this.defaultRules = defaultRules;
  }

  @Override
  public List<DemoDetectionResult> importAndDetect(List<DemoCommentItem> items) {
    return items.stream().map(item -> analyze(item)).toList();
  }

  private DemoDetectionResult analyze(DemoCommentItem item) {
    DetectionResult result =
        detectCommentUseCase.detect(new DetectCommentCommand(item.text(), defaultRules));
    return new DemoDetectionResult(
        item.externalCommentId(),
        result.status(),
        (int) Math.round(result.score() * 100),
        result.reasonCodes());
  }
}

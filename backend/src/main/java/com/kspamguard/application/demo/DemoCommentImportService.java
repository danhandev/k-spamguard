package com.kspamguard.application.demo;

import com.kspamguard.application.detection.DetectCommentCommand;
import com.kspamguard.application.detection.DetectCommentUseCase;
import com.kspamguard.application.port.out.CommentPersistencePort;
import com.kspamguard.application.port.out.DetectionResultPersistencePort;
import com.kspamguard.application.port.out.ModerationQueuePersistencePort;
import com.kspamguard.application.port.out.SpamRuleQueryPort;
import com.kspamguard.application.rule.SpamRuleView;
import com.kspamguard.domain.detection.DetectionResult;
import com.kspamguard.domain.detection.DetectionStatus;
import com.kspamguard.domain.rule.SpamRule;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DemoCommentImportService implements DemoCommentImportUseCase {

  private final DetectCommentUseCase detectCommentUseCase;
  private final SpamRuleQueryPort spamRuleQueryPort;
  private final CommentPersistencePort commentPersistencePort;
  private final DetectionResultPersistencePort detectionResultPersistencePort;
  private final ModerationQueuePersistencePort moderationQueuePersistencePort;

  public DemoCommentImportService(
      DetectCommentUseCase detectCommentUseCase,
      SpamRuleQueryPort spamRuleQueryPort,
      CommentPersistencePort commentPersistencePort,
      DetectionResultPersistencePort detectionResultPersistencePort,
      ModerationQueuePersistencePort moderationQueuePersistencePort) {
    this.detectCommentUseCase = detectCommentUseCase;
    this.spamRuleQueryPort = spamRuleQueryPort;
    this.commentPersistencePort = commentPersistencePort;
    this.detectionResultPersistencePort = detectionResultPersistencePort;
    this.moderationQueuePersistencePort = moderationQueuePersistencePort;
  }

  @Override
  @Transactional
  public List<DemoDetectionResult> importAndDetect(List<DemoCommentItem> items) {
    List<SpamRule> rules =
        spamRuleQueryPort.findAllEnabled().stream().map(this::toSpamRule).toList();
    return items.stream().map(item -> analyzeAndPersist(item, rules)).toList();
  }

  private DemoDetectionResult analyzeAndPersist(DemoCommentItem item, List<SpamRule> rules) {
    DetectionResult result =
        detectCommentUseCase.detect(new DetectCommentCommand(item.text(), rules));

    int score = (int) Math.round(result.score() * 100);
    Instant now = Instant.now();

    Long commentId =
        commentPersistencePort.save(
            item.externalCommentId(), item.username(), item.text(), result.normalizedText(), now);

    detectionResultPersistencePort.save(
        commentId, result.status(), score, result.reasonCodes(), now);

    if (result.status() != DetectionStatus.SAFE) {
      String action = result.status() == DetectionStatus.SPAM ? "HIDE" : "REVIEW";
      moderationQueuePersistencePort.enqueue(commentId, action, now);
    }

    return new DemoDetectionResult(
        item.externalCommentId(), result.status(), score, result.reasonCodes());
  }

  private SpamRule toSpamRule(SpamRuleView view) {
    return "REGEX".equals(view.ruleType())
        ? SpamRule.regex(view.ruleCode(), view.pattern(), view.threshold())
        : SpamRule.keyword(view.ruleCode(), view.pattern(), view.threshold());
  }
}

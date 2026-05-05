package com.kspamguard.application.detection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kspamguard.domain.detection.Comment;
import com.kspamguard.domain.detection.DetectionResult;
import com.kspamguard.domain.detection.DetectionStatus;
import com.kspamguard.domain.detection.RuleMatch;
import com.kspamguard.domain.detection.SpamDetector;
import com.kspamguard.domain.rule.SpamRule;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DetectCommentServiceTest {

  @Mock SpamDetector spamDetector;

  @InjectMocks DetectCommentService service;

  @Test
  void delegatesDetectionToSpamDetector() {
    var rule = SpamRule.keyword("r1", "스팸", 0.9);
    var rules = List.of(rule);
    var command = new DetectCommentCommand("스팸 댓글", rules);
    var ruleMatch = new RuleMatch(rule, "스팸");
    var expected = new DetectionResult(DetectionStatus.SPAM, 0.9, List.of(ruleMatch));

    when(spamDetector.detect(new Comment("스팸 댓글"), rules)).thenReturn(expected);

    var result = service.detect(command);

    assertThat(result.status()).isEqualTo(DetectionStatus.SPAM);
    verify(spamDetector).detect(new Comment("스팸 댓글"), rules);
  }

  @Test
  void emptyRules_returnsSAFE() {
    var command = new DetectCommentCommand("정상 댓글", List.of());
    var expected = new DetectionResult(DetectionStatus.SAFE, 0.0, List.of());

    when(spamDetector.detect(new Comment("정상 댓글"), List.of())).thenReturn(expected);

    var result = service.detect(command);

    assertThat(result.status()).isEqualTo(DetectionStatus.SAFE);
  }
}

package com.kspamguard.application.detection;

import com.kspamguard.domain.detection.Comment;
import com.kspamguard.domain.detection.DetectionResult;
import com.kspamguard.domain.detection.SpamDetector;
import com.kspamguard.domain.detection.Verdict;
import com.kspamguard.domain.rule.SpamRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DetectCommentServiceTest {

    @Mock
    SpamDetector spamDetector;

    @InjectMocks
    DetectCommentService service;

    @Test
    void delegatesDetectionToSpamDetector() {
        var rules = List.of(SpamRule.keyword("r1", "스팸", 0.9));
        var command = new DetectCommentCommand("스팸 댓글", rules);
        var expected = new DetectionResult(Verdict.SPAM, 0.9, rules);

        when(spamDetector.detect(new Comment("스팸 댓글"), rules)).thenReturn(expected);

        var result = service.detect(command);

        assertThat(result.verdict()).isEqualTo(Verdict.SPAM);
        verify(spamDetector).detect(new Comment("스팸 댓글"), rules);
    }

    @Test
    void emptyRules_returnsSAFE() {
        var command = new DetectCommentCommand("정상 댓글", List.of());
        var expected = new DetectionResult(Verdict.SAFE, 0.0, List.of());

        when(spamDetector.detect(new Comment("정상 댓글"), List.of())).thenReturn(expected);

        var result = service.detect(command);

        assertThat(result.verdict()).isEqualTo(Verdict.SAFE);
    }
}

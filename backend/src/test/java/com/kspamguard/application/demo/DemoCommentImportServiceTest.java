package com.kspamguard.application.demo;

import com.kspamguard.application.detection.DetectCommentCommand;
import com.kspamguard.application.detection.DetectCommentUseCase;
import com.kspamguard.domain.detection.DetectionResult;
import com.kspamguard.domain.detection.DetectionStatus;
import com.kspamguard.domain.detection.RuleMatch;
import com.kspamguard.domain.rule.SpamRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DemoCommentImportServiceTest {

    @Mock
    DetectCommentUseCase detectCommentUseCase;

    DemoCommentImportService service;

    SpamRule dmRule = SpamRule.regex("DM_LURE", "dm\\s*주세요", 0.45);
    SpamRule couponRule = SpamRule.regex("OBFUSCATED_FREE_COUPON", "무.{0,5}쿠폰", 0.45);

    @BeforeEach
    void setUp() {
        service = new DemoCommentImportService(detectCommentUseCase, List.of(dmRule, couponRule));
    }

    @Test
    void spamComment_returnsSpamStatusAndReasonCodes() {
        var spamResult = new DetectionResult(
                DetectionStatus.SPAM,
                0.9,
                List.of(new RuleMatch(couponRule, "무 ㄹㅛ 쿠폰"), new RuleMatch(dmRule, "dm 주세요"))
        );
        when(detectCommentUseCase.detect(any(DetectCommentCommand.class))).thenReturn(spamResult);

        var items = List.of(new DemoCommentItem("demo-001", "spam_user", "무 ㄹㅛ 쿠폰 받으실 분 DM 주세요"));
        var results = service.importAndDetect(items);

        assertThat(results).hasSize(1);
        var r = results.get(0);
        assertThat(r.externalCommentId()).isEqualTo("demo-001");
        assertThat(r.status()).isEqualTo(DetectionStatus.SPAM);
        assertThat(r.score()).isEqualTo(90);
        assertThat(r.reasonCodes()).containsExactlyInAnyOrder("OBFUSCATED_FREE_COUPON", "DM_LURE");
    }

    @Test
    void safeComment_returnsSafeStatusAndEmptyReasonCodes() {
        var safeResult = new DetectionResult(DetectionStatus.SAFE, 0.0, List.of());
        when(detectCommentUseCase.detect(any(DetectCommentCommand.class))).thenReturn(safeResult);

        var items = List.of(new DemoCommentItem("demo-002", "normal_user", "영상 잘 봤어요. 설명이 정말 좋네요!"));
        var results = service.importAndDetect(items);

        assertThat(results).hasSize(1);
        var r = results.get(0);
        assertThat(r.status()).isEqualTo(DetectionStatus.SAFE);
        assertThat(r.score()).isZero();
        assertThat(r.reasonCodes()).isEmpty();
    }

    @Test
    void multipleComments_returnsResultForEach() {
        var spamResult = new DetectionResult(DetectionStatus.SPAM, 0.9, List.of(new RuleMatch(dmRule, "dm 주세요")));
        var safeResult = new DetectionResult(DetectionStatus.SAFE, 0.0, List.of());
        when(detectCommentUseCase.detect(any(DetectCommentCommand.class)))
                .thenReturn(spamResult)
                .thenReturn(safeResult);

        var items = List.of(
                new DemoCommentItem("id-1", "spammer", "DM 주세요"),
                new DemoCommentItem("id-2", "normal", "좋은 영상이에요")
        );
        var results = service.importAndDetect(items);

        assertThat(results).hasSize(2);
        assertThat(results.get(0).status()).isEqualTo(DetectionStatus.SPAM);
        assertThat(results.get(1).status()).isEqualTo(DetectionStatus.SAFE);
    }

    @Test
    void emptyCommentList_returnsEmptyResults() {
        var results = service.importAndDetect(List.of());
        assertThat(results).isEmpty();
    }

    @Test
    void score_isConvertedTo0to100IntRange() {
        var result = new DetectionResult(DetectionStatus.SUSPECT, 0.456, List.of());
        when(detectCommentUseCase.detect(any(DetectCommentCommand.class))).thenReturn(result);

        var items = List.of(new DemoCommentItem("id-1", "u", "text"));
        var results = service.importAndDetect(items);

        assertThat(results.get(0).score()).isEqualTo(46); // Math.round(0.456 * 100)
    }

    @Test
    void idempotent_sameInputProduceSameOutput() {
        var spamResult = new DetectionResult(
                DetectionStatus.SPAM, 0.9,
                List.of(new RuleMatch(dmRule, "dm 주세요"))
        );
        when(detectCommentUseCase.detect(any(DetectCommentCommand.class))).thenReturn(spamResult);

        var items = List.of(new DemoCommentItem("demo-001", "spammer", "DM 주세요"));
        var first = service.importAndDetect(items);
        var second = service.importAndDetect(items);

        assertThat(first.get(0).score()).isEqualTo(second.get(0).score());
        assertThat(first.get(0).status()).isEqualTo(second.get(0).status());
    }
}

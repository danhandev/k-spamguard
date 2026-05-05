package com.kspamguard.application.demo;

import com.kspamguard.domain.detection.DetectionStatus;
import java.util.List;

public record DemoDetectionResult(
    String externalCommentId, DetectionStatus status, int score, List<String> reasonCodes) {}

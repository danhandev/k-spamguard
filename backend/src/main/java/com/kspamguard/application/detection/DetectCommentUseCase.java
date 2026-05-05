package com.kspamguard.application.detection;

import com.kspamguard.domain.detection.DetectionResult;

public interface DetectCommentUseCase {
  DetectionResult detect(DetectCommentCommand command);
}

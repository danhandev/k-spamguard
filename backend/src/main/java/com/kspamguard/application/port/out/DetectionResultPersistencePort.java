package com.kspamguard.application.port.out;

import com.kspamguard.domain.detection.DetectionStatus;
import java.time.Instant;
import java.util.List;

public interface DetectionResultPersistencePort {
  void save(
      Long commentId,
      DetectionStatus status,
      int score,
      List<String> reasonCodes,
      Instant detectedAt);
}

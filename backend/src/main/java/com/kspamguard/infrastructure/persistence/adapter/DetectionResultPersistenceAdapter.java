package com.kspamguard.infrastructure.persistence.adapter;

import com.kspamguard.application.port.out.DetectionResultPersistencePort;
import com.kspamguard.domain.detection.DetectionStatus;
import com.kspamguard.infrastructure.persistence.entity.DetectionResultJpaEntity;
import com.kspamguard.infrastructure.persistence.repository.DetectionResultJpaRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DetectionResultPersistenceAdapter implements DetectionResultPersistencePort {

  private final DetectionResultJpaRepository repository;

  public DetectionResultPersistenceAdapter(DetectionResultJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public void save(
      Long commentId,
      DetectionStatus status,
      int score,
      List<String> reasonCodes,
      Instant detectedAt) {
    DetectionResultJpaEntity entity =
        new DetectionResultJpaEntity(commentId, status.name(), score, reasonCodes, detectedAt);
    repository.save(entity);
  }
}

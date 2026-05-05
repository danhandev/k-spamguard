package com.kspamguard.infrastructure.persistence.adapter;

import com.kspamguard.application.port.out.ModerationQueuePersistencePort;
import com.kspamguard.infrastructure.persistence.entity.ModerationQueueItemJpaEntity;
import com.kspamguard.infrastructure.persistence.repository.ModerationQueueItemJpaRepository;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class ModerationQueuePersistenceAdapter implements ModerationQueuePersistencePort {

  private final ModerationQueueItemJpaRepository repository;

  public ModerationQueuePersistenceAdapter(ModerationQueueItemJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public void enqueue(Long commentId, String recommendedAction, Instant createdAt) {
    ModerationQueueItemJpaEntity entity =
        new ModerationQueueItemJpaEntity(commentId, recommendedAction, "PENDING_REVIEW", createdAt);
    repository.save(entity);
  }
}

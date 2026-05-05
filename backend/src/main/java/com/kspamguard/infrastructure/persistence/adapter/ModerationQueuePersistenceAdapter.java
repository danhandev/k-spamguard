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
    repository.save(
        new ModerationQueueItemJpaEntity(
            commentId, recommendedAction, "PENDING_REVIEW", createdAt));
  }

  @Override
  public void review(Long id, String status, Instant reviewedAt) {
    ModerationQueueItemJpaEntity entity =
        repository
            .findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Moderation item not found: " + id));
    entity.markReviewed(status, reviewedAt);
    repository.save(entity);
  }
}

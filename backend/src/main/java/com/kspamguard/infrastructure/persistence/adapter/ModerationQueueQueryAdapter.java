package com.kspamguard.infrastructure.persistence.adapter;

import com.kspamguard.application.moderation.ModerationQueueItemView;
import com.kspamguard.application.port.out.ModerationQueueQueryPort;
import com.kspamguard.infrastructure.persistence.entity.ModerationQueueItemJpaEntity;
import com.kspamguard.infrastructure.persistence.repository.ModerationQueueItemJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class ModerationQueueQueryAdapter implements ModerationQueueQueryPort {

  private final ModerationQueueItemJpaRepository repository;

  public ModerationQueueQueryAdapter(ModerationQueueItemJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<ModerationQueueItemView> findByStatus(String status) {
    return repository.findByStatus(status).stream().map(this::toView).toList();
  }

  @Override
  public Optional<ModerationQueueItemView> findById(Long id) {
    return repository.findById(id).map(this::toView);
  }

  private ModerationQueueItemView toView(ModerationQueueItemJpaEntity e) {
    return new ModerationQueueItemView(
        e.getId(), e.getCommentId(), e.getRecommendedAction(), e.getStatus(), e.getCreatedAt());
  }
}

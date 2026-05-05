package com.kspamguard.infrastructure.persistence.adapter;

import com.kspamguard.application.port.out.CommentPersistencePort;
import com.kspamguard.infrastructure.persistence.entity.CommentJpaEntity;
import com.kspamguard.infrastructure.persistence.repository.CommentJpaRepository;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class CommentPersistenceAdapter implements CommentPersistencePort {

  private final CommentJpaRepository repository;

  public CommentPersistenceAdapter(CommentJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public Long save(
      String externalCommentId,
      String username,
      String originalText,
      String normalizedText,
      Instant receivedAt) {
    CommentJpaEntity entity =
        new CommentJpaEntity(externalCommentId, username, originalText, normalizedText, receivedAt);
    return repository.save(entity).getId();
  }
}

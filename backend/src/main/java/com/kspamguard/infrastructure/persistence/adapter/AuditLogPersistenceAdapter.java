package com.kspamguard.infrastructure.persistence.adapter;

import com.kspamguard.application.port.out.AuditLogPersistencePort;
import com.kspamguard.infrastructure.persistence.entity.AuditLogJpaEntity;
import com.kspamguard.infrastructure.persistence.repository.AuditLogJpaRepository;
import java.time.Instant;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class AuditLogPersistenceAdapter implements AuditLogPersistencePort {

  private final AuditLogJpaRepository repository;

  public AuditLogPersistenceAdapter(AuditLogJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public void log(
      String eventType,
      String targetType,
      Long targetId,
      Map<String, Object> metadata,
      Instant createdAt) {
    AuditLogJpaEntity entity =
        new AuditLogJpaEntity(eventType, targetType, targetId, metadata, createdAt);
    repository.save(entity);
  }
}

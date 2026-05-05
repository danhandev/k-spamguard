package com.kspamguard.infrastructure.persistence.repository;

import com.kspamguard.infrastructure.persistence.entity.ModerationQueueItemJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModerationQueueItemJpaRepository
    extends JpaRepository<ModerationQueueItemJpaEntity, Long> {
  List<ModerationQueueItemJpaEntity> findByStatus(String status);
}

package com.kspamguard.infrastructure.persistence.repository;

import com.kspamguard.infrastructure.persistence.entity.ModerationQueueItemJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModerationQueueItemJpaRepository
    extends JpaRepository<ModerationQueueItemJpaEntity, Long> {}

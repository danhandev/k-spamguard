package com.kspamguard.infrastructure.persistence.repository;

import com.kspamguard.infrastructure.persistence.entity.DetectionResultJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DetectionResultJpaRepository
    extends JpaRepository<DetectionResultJpaEntity, Long> {}

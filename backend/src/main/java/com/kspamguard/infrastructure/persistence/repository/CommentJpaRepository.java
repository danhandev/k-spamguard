package com.kspamguard.infrastructure.persistence.repository;

import com.kspamguard.infrastructure.persistence.entity.CommentJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentJpaRepository extends JpaRepository<CommentJpaEntity, Long> {}

package com.kspamguard.infrastructure.persistence.repository;

import com.kspamguard.infrastructure.persistence.entity.SpamRuleJpaEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpamRuleJpaRepository extends JpaRepository<SpamRuleJpaEntity, Long> {
  List<SpamRuleJpaEntity> findByEnabledTrue();

  boolean existsByRuleCode(String ruleCode);
}

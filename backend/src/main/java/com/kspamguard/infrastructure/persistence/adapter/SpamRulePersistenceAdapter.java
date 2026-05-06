package com.kspamguard.infrastructure.persistence.adapter;

import com.kspamguard.application.port.out.SpamRulePersistencePort;
import com.kspamguard.infrastructure.persistence.entity.SpamRuleJpaEntity;
import com.kspamguard.infrastructure.persistence.repository.SpamRuleJpaRepository;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class SpamRulePersistenceAdapter implements SpamRulePersistencePort {

  private final SpamRuleJpaRepository repository;

  public SpamRulePersistenceAdapter(SpamRuleJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public Long create(
      String ruleCode, String ruleType, String pattern, double threshold, Instant now) {
    SpamRuleJpaEntity entity =
        new SpamRuleJpaEntity(
            ruleCode, ruleType, pattern, BigDecimal.valueOf(threshold), true, now, now);
    return repository.save(entity).getId();
  }

  @Override
  public void update(Long id, String pattern, double threshold, boolean enabled, Instant now) {
    repository
        .findById(id)
        .orElseThrow()
        .applyUpdate(pattern, BigDecimal.valueOf(threshold), enabled, now);
  }

  @Override
  public void disable(Long id, Instant now) {
    repository.findById(id).orElseThrow().disable(now);
  }
}

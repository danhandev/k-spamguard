package com.kspamguard.infrastructure.persistence.adapter;

import com.kspamguard.application.port.out.SpamRuleQueryPort;
import com.kspamguard.application.rule.SpamRuleView;
import com.kspamguard.infrastructure.persistence.entity.SpamRuleJpaEntity;
import com.kspamguard.infrastructure.persistence.repository.SpamRuleJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SpamRuleQueryAdapter implements SpamRuleQueryPort {

  private final SpamRuleJpaRepository repository;

  public SpamRuleQueryAdapter(SpamRuleJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<SpamRuleView> findAllEnabled() {
    return repository.findByEnabledTrue().stream().map(this::toView).toList();
  }

  @Override
  public Optional<SpamRuleView> findById(Long id) {
    return repository.findById(id).map(this::toView);
  }

  @Override
  public boolean existsByRuleCode(String ruleCode) {
    return repository.existsByRuleCode(ruleCode);
  }

  private SpamRuleView toView(SpamRuleJpaEntity entity) {
    return new SpamRuleView(
        entity.getId(),
        entity.getRuleCode(),
        entity.getRuleType(),
        entity.getPattern(),
        entity.getThreshold().doubleValue(),
        entity.isEnabled(),
        entity.getCreatedAt());
  }
}

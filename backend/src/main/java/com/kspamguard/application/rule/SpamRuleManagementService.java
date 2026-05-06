package com.kspamguard.application.rule;

import com.kspamguard.application.port.out.AuditLogPersistencePort;
import com.kspamguard.application.port.out.SpamRulePersistencePort;
import com.kspamguard.application.port.out.SpamRuleQueryPort;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SpamRuleManagementService
    implements CreateSpamRuleUseCase,
        UpdateSpamRuleUseCase,
        DeleteSpamRuleUseCase,
        ListSpamRulesUseCase {

  private final SpamRuleQueryPort queryPort;
  private final SpamRulePersistencePort persistencePort;
  private final AuditLogPersistencePort auditLogPort;

  public SpamRuleManagementService(
      SpamRuleQueryPort queryPort,
      SpamRulePersistencePort persistencePort,
      AuditLogPersistencePort auditLogPort) {
    this.queryPort = queryPort;
    this.persistencePort = persistencePort;
    this.auditLogPort = auditLogPort;
  }

  @Override
  @Transactional
  public SpamRuleView create(CreateSpamRuleCommand command) {
    if (queryPort.existsByRuleCode(command.ruleCode())) {
      throw new DuplicateRuleCodeException(command.ruleCode());
    }
    Instant now = Instant.now();
    Long id =
        persistencePort.create(
            command.ruleCode(), command.ruleType(), command.pattern(), command.threshold(), now);
    auditLogPort.log(
        "rule_create",
        "spam_rule",
        id,
        Map.of(
            "rule_code", command.ruleCode(),
            "pattern", command.pattern(),
            "threshold", command.threshold()),
        now);
    return queryPort.findById(id).orElseThrow();
  }

  @Override
  @Transactional
  public void update(UpdateSpamRuleCommand command) {
    SpamRuleView existing =
        queryPort
            .findById(command.id())
            .orElseThrow(() -> new SpamRuleNotFoundException(command.id()));
    Instant now = Instant.now();
    persistencePort.update(
        command.id(), command.pattern(), command.threshold(), command.enabled(), now);
    auditLogPort.log(
        "rule_update",
        "spam_rule",
        command.id(),
        Map.of(
            "rule_code", existing.ruleCode(),
            "pattern", command.pattern(),
            "threshold", command.threshold()),
        now);
  }

  @Override
  @Transactional
  public void delete(Long id) {
    SpamRuleView existing =
        queryPort.findById(id).orElseThrow(() -> new SpamRuleNotFoundException(id));
    Instant now = Instant.now();
    persistencePort.disable(id, now);
    auditLogPort.log("rule_delete", "spam_rule", id, Map.of("rule_code", existing.ruleCode()), now);
  }

  @Override
  public List<SpamRuleView> listEnabled() {
    return queryPort.findAllEnabled();
  }
}

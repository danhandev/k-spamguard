package com.kspamguard.application.rule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kspamguard.application.port.out.AuditLogPersistencePort;
import com.kspamguard.application.port.out.SpamRulePersistencePort;
import com.kspamguard.application.port.out.SpamRuleQueryPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpamRuleManagementServiceTest {

  @Mock SpamRuleQueryPort queryPort;
  @Mock SpamRulePersistencePort persistencePort;
  @Mock AuditLogPersistencePort auditLogPort;

  SpamRuleManagementService service;

  SpamRuleView sampleView =
      new SpamRuleView(1L, "DM_LURE", "REGEX", "dm\\s*주세요", 0.45, true, Instant.now());

  @BeforeEach
  void setUp() {
    service = new SpamRuleManagementService(queryPort, persistencePort, auditLogPort);
  }

  @Test
  void createRule_savesAndLogsAudit() {
    when(queryPort.existsByRuleCode("DM_LURE")).thenReturn(false);
    when(persistencePort.create(anyString(), anyString(), anyString(), anyDouble(), any()))
        .thenReturn(1L);
    when(queryPort.findById(1L)).thenReturn(Optional.of(sampleView));

    SpamRuleView result =
        service.create(new CreateSpamRuleCommand("DM_LURE", "REGEX", "dm\\s*주세요", 0.45));

    assertThat(result.ruleCode()).isEqualTo("DM_LURE");
    verify(persistencePort).create(eq("DM_LURE"), eq("REGEX"), eq("dm\\s*주세요"), eq(0.45), any());
    verify(auditLogPort).log(eq("rule_create"), eq("spam_rule"), eq(1L), any(), any());
  }

  @Test
  void createRule_duplicateCode_throws409() {
    when(queryPort.existsByRuleCode("DM_LURE")).thenReturn(true);

    assertThatThrownBy(
            () -> service.create(new CreateSpamRuleCommand("DM_LURE", "REGEX", "dm", 0.4)))
        .isInstanceOf(DuplicateRuleCodeException.class);

    verify(persistencePort, never())
        .create(anyString(), anyString(), anyString(), anyDouble(), any());
  }

  @Test
  void updateRule_notFound_throws404() {
    when(queryPort.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> service.update(new UpdateSpamRuleCommand(99L, "new-pattern", 0.5, true)))
        .isInstanceOf(SpamRuleNotFoundException.class);

    verify(persistencePort, never())
        .update(any(), anyString(), anyDouble(), any(Boolean.class), any());
  }

  @Test
  void updateRule_updatesFieldsAndLogsAudit() {
    when(queryPort.findById(1L)).thenReturn(Optional.of(sampleView));

    service.update(new UpdateSpamRuleCommand(1L, "new-pattern", 0.5, false));

    verify(persistencePort).update(eq(1L), eq("new-pattern"), eq(0.5), eq(false), any());
    verify(auditLogPort).log(eq("rule_update"), eq("spam_rule"), eq(1L), any(), any());
  }

  @Test
  void deleteRule_disablesAndLogsAudit() {
    when(queryPort.findById(1L)).thenReturn(Optional.of(sampleView));

    service.delete(1L);

    verify(persistencePort).disable(eq(1L), any());
    verify(auditLogPort).log(eq("rule_delete"), eq("spam_rule"), eq(1L), any(), any());
  }

  @Test
  void deleteRule_notFound_throws404() {
    when(queryPort.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(SpamRuleNotFoundException.class);

    verify(persistencePort, never()).disable(any(), any());
  }

  @Test
  void listRules_returnsOnlyEnabled() {
    SpamRuleView disabled =
        new SpamRuleView(2L, "OLD_RULE", "KEYWORD", "spam", 0.3, false, Instant.now());
    when(queryPort.findAllEnabled()).thenReturn(List.of(sampleView));

    List<SpamRuleView> result = service.listEnabled();

    assertThat(result).containsExactly(sampleView);
    assertThat(result).doesNotContain(disabled);
  }
}

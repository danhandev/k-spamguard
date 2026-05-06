package com.kspamguard.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.kspamguard.application.rule.SpamRuleView;
import com.kspamguard.infrastructure.persistence.adapter.SpamRulePersistenceAdapter;
import com.kspamguard.infrastructure.persistence.adapter.SpamRuleQueryAdapter;
import com.kspamguard.infrastructure.persistence.repository.SpamRuleJpaRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@Transactional
class SpamRuleIntegrationTest {

  @Container @ServiceConnection static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired SpamRulePersistenceAdapter persistenceAdapter;
  @Autowired SpamRuleQueryAdapter queryAdapter;
  @Autowired SpamRuleJpaRepository repository;

  @Test
  void seedData_loadedFromV7() {
    List<SpamRuleView> enabled = queryAdapter.findAllEnabled();
    assertThat(enabled).hasSize(7);
    assertThat(enabled).extracting(SpamRuleView::ruleCode)
        .containsExactlyInAnyOrder(
            "OBFUSCATED_FREE_COUPON",
            "DM_LURE",
            "FOLLOW_LURE",
            "SUBSCRIBE_LURE",
            "PHISHING_LINK",
            "AD_EVENT",
            "FREE_GIFT");
  }

  @Test
  void createAndFindEnabled() {
    long before = queryAdapter.findAllEnabled().size();
    Instant now = Instant.now();

    Long id = persistenceAdapter.create("TEST_RULE", "KEYWORD", "테스트", 0.3, now);

    assertThat(id).isNotNull();
    List<SpamRuleView> enabled = queryAdapter.findAllEnabled();
    assertThat(enabled).hasSize((int) before + 1);
    assertThat(enabled).anyMatch(v -> "TEST_RULE".equals(v.ruleCode()));
  }

  @Test
  void disabledRule_notReturnedInEnabledList() {
    Long id = persistenceAdapter.create("TEMP_RULE", "KEYWORD", "임시", 0.2, Instant.now());
    long before = queryAdapter.findAllEnabled().size();

    persistenceAdapter.disable(id, Instant.now());

    assertThat(queryAdapter.findAllEnabled()).hasSize((int) before - 1);
    assertThat(queryAdapter.findById(id)).isPresent();
    assertThat(queryAdapter.findById(id).get().enabled()).isFalse();
  }

  @Test
  void update_changesPatternAndThreshold() {
    Long id = persistenceAdapter.create("UPD_RULE", "REGEX", "old-pattern", 0.3, Instant.now());

    persistenceAdapter.update(id, "new-pattern", 0.7, true, Instant.now());

    SpamRuleView updated = queryAdapter.findById(id).orElseThrow();
    assertThat(updated.pattern()).isEqualTo("new-pattern");
    assertThat(updated.threshold()).isEqualTo(0.7);
  }

  @Test
  void existsByRuleCode_returnsTrueForSeedData() {
    assertThat(queryAdapter.existsByRuleCode("DM_LURE")).isTrue();
    assertThat(queryAdapter.existsByRuleCode("NON_EXISTENT")).isFalse();
  }
}

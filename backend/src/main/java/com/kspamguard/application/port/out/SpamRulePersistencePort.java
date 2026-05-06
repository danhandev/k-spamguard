package com.kspamguard.application.port.out;

import java.time.Instant;

public interface SpamRulePersistencePort {
  Long create(String ruleCode, String ruleType, String pattern, double threshold, Instant now);

  void update(Long id, String pattern, double threshold, boolean enabled, Instant now);

  void disable(Long id, Instant now);
}

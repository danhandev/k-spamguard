package com.kspamguard.application.port.out;

import com.kspamguard.application.rule.SpamRuleView;
import java.util.List;
import java.util.Optional;

public interface SpamRuleQueryPort {
  List<SpamRuleView> findAllEnabled();

  Optional<SpamRuleView> findById(Long id);

  boolean existsByRuleCode(String ruleCode);
}

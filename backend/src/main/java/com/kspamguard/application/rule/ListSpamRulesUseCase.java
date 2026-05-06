package com.kspamguard.application.rule;

import java.util.List;

public interface ListSpamRulesUseCase {
  List<SpamRuleView> listEnabled();
}

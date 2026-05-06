package com.kspamguard.application.rule;

public interface CreateSpamRuleUseCase {
  SpamRuleView create(CreateSpamRuleCommand command);
}

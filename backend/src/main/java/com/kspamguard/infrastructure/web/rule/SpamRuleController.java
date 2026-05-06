package com.kspamguard.infrastructure.web.rule;

import com.kspamguard.application.rule.CreateSpamRuleCommand;
import com.kspamguard.application.rule.CreateSpamRuleUseCase;
import com.kspamguard.application.rule.DeleteSpamRuleUseCase;
import com.kspamguard.application.rule.DuplicateRuleCodeException;
import com.kspamguard.application.rule.ListSpamRulesUseCase;
import com.kspamguard.application.rule.SpamRuleNotFoundException;
import com.kspamguard.application.rule.UpdateSpamRuleCommand;
import com.kspamguard.application.rule.UpdateSpamRuleUseCase;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rules")
public class SpamRuleController {

  private final ListSpamRulesUseCase listUseCase;
  private final CreateSpamRuleUseCase createUseCase;
  private final UpdateSpamRuleUseCase updateUseCase;
  private final DeleteSpamRuleUseCase deleteUseCase;

  public SpamRuleController(
      ListSpamRulesUseCase listUseCase,
      CreateSpamRuleUseCase createUseCase,
      UpdateSpamRuleUseCase updateUseCase,
      DeleteSpamRuleUseCase deleteUseCase) {
    this.listUseCase = listUseCase;
    this.createUseCase = createUseCase;
    this.updateUseCase = updateUseCase;
    this.deleteUseCase = deleteUseCase;
  }

  @GetMapping
  public List<SpamRuleResponse> listEnabled() {
    return listUseCase.listEnabled().stream().map(SpamRuleResponse::from).toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public SpamRuleResponse create(@Valid @RequestBody CreateSpamRuleRequest request) {
    return SpamRuleResponse.from(
        createUseCase.create(
            new CreateSpamRuleCommand(
                request.ruleCode(), request.ruleType(), request.pattern(), request.threshold())));
  }

  @PatchMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void update(@PathVariable Long id, @Valid @RequestBody UpdateSpamRuleRequest request) {
    updateUseCase.update(
        new UpdateSpamRuleCommand(id, request.pattern(), request.threshold(), request.enabled()));
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void delete(@PathVariable Long id) {
    deleteUseCase.delete(id);
  }

  @ExceptionHandler(SpamRuleNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public void handleNotFound() {}

  @ExceptionHandler(DuplicateRuleCodeException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public void handleDuplicate() {}
}

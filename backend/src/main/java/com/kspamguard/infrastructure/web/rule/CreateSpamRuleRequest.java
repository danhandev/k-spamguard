package com.kspamguard.infrastructure.web.rule;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateSpamRuleRequest(
    @NotBlank @Size(max = 60) String ruleCode,
    @NotBlank @Pattern(regexp = "REGEX|KEYWORD") String ruleType,
    @NotBlank @Size(max = 500) String pattern,
    @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double threshold) {}

package com.kspamguard.infrastructure.web.rule;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateSpamRuleRequest(
    @NotBlank @Size(max = 500) String pattern,
    @NotNull @DecimalMin("0.0") @DecimalMax("1.0") Double threshold,
    @NotNull Boolean enabled) {}

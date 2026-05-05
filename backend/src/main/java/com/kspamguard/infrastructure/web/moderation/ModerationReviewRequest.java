package com.kspamguard.infrastructure.web.moderation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kspamguard.application.moderation.ModerationAction;
import jakarta.validation.constraints.NotNull;

public record ModerationReviewRequest(
    @NotNull ModerationAction action, @JsonProperty("reviewer_note") String reviewerNote) {}

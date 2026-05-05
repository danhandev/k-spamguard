package com.kspamguard.application.moderation;

import java.time.Instant;

public record ModerationQueueItemView(
    Long id, Long commentId, String recommendedAction, String status, Instant createdAt) {}

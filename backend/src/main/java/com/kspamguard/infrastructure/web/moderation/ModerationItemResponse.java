package com.kspamguard.infrastructure.web.moderation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.kspamguard.application.moderation.ModerationQueueItemView;
import java.time.Instant;

public record ModerationItemResponse(
    Long id,
    @JsonProperty("comment_id") Long commentId,
    @JsonProperty("recommended_action") String recommendedAction,
    String status,
    @JsonProperty("created_at") Instant createdAt) {

  public static ModerationItemResponse from(ModerationQueueItemView view) {
    return new ModerationItemResponse(
        view.id(), view.commentId(), view.recommendedAction(), view.status(), view.createdAt());
  }
}

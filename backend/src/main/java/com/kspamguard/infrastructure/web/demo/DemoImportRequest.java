package com.kspamguard.infrastructure.web.demo;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record DemoImportRequest(@NotNull List<CommentEntry> comments) {

  public record CommentEntry(
      @JsonProperty("external_comment_id") String externalCommentId,
      String username,
      String text) {}
}

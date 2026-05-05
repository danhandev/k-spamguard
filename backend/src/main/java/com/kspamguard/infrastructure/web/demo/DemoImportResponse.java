package com.kspamguard.infrastructure.web.demo;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record DemoImportResponse(
    @JsonProperty("imported_count") int importedCount, List<ResultEntry> results) {

  public record ResultEntry(
      @JsonProperty("external_comment_id") String externalCommentId,
      String status,
      int score,
      @JsonProperty("reason_codes") List<String> reasonCodes) {}
}

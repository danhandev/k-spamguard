package com.kspamguard.application.port.out;

import java.time.Instant;

public interface CommentPersistencePort {
  Long save(
      String externalCommentId,
      String username,
      String originalText,
      String normalizedText,
      Instant receivedAt);
}

package com.kspamguard.application.port.out;

import java.time.Instant;

public interface ModerationQueuePersistencePort {
  void enqueue(Long commentId, String recommendedAction, Instant createdAt);

  void review(Long id, String status, Instant reviewedAt);
}

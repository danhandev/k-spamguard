package com.kspamguard.application.port.out;

import java.time.Instant;
import java.util.Map;

public interface AuditLogPersistencePort {
  void log(
      String eventType,
      String targetType,
      Long targetId,
      Map<String, Object> metadata,
      Instant createdAt);
}

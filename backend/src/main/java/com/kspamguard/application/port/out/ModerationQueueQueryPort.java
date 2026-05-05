package com.kspamguard.application.port.out;

import com.kspamguard.application.moderation.ModerationQueueItemView;
import java.util.List;
import java.util.Optional;

public interface ModerationQueueQueryPort {
  List<ModerationQueueItemView> findByStatus(String status);

  Optional<ModerationQueueItemView> findById(Long id);
}

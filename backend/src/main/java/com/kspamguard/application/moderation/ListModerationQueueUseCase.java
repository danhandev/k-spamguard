package com.kspamguard.application.moderation;

import java.util.List;

public interface ListModerationQueueUseCase {
  List<ModerationQueueItemView> listPending();
}

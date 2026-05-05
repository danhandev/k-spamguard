package com.kspamguard.application.moderation;

import com.kspamguard.application.port.out.AuditLogPersistencePort;
import com.kspamguard.application.port.out.ModerationQueuePersistencePort;
import com.kspamguard.application.port.out.ModerationQueueQueryPort;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewModerationItemService
    implements ReviewModerationItemUseCase, ListModerationQueueUseCase {

  private final ModerationQueueQueryPort queryPort;
  private final ModerationQueuePersistencePort persistencePort;
  private final AuditLogPersistencePort auditLogPort;

  public ReviewModerationItemService(
      ModerationQueueQueryPort queryPort,
      ModerationQueuePersistencePort persistencePort,
      AuditLogPersistencePort auditLogPort) {
    this.queryPort = queryPort;
    this.persistencePort = persistencePort;
    this.auditLogPort = auditLogPort;
  }

  @Override
  public List<ModerationQueueItemView> listPending() {
    return queryPort.findByStatus("PENDING_REVIEW");
  }

  @Override
  @Transactional
  public void review(ReviewModerationItemCommand command) {
    ModerationQueueItemView item =
        queryPort
            .findById(command.id())
            .orElseThrow(() -> new ModerationItemNotFoundException(command.id()));

    if (!"PENDING_REVIEW".equals(item.status())) {
      throw new ModerationItemAlreadyReviewedException(command.id(), item.status());
    }

    Instant now = Instant.now();
    persistencePort.review(command.id(), command.action().name(), now);

    String eventType =
        command.action() == ModerationAction.APPROVED ? "moderation_approve" : "moderation_reject";
    Map<String, Object> metadata =
        command.reviewerNote() != null ? Map.of("reviewer_note", command.reviewerNote()) : Map.of();
    auditLogPort.log(eventType, "moderation_queue_item", command.id(), metadata, now);
  }
}

package com.kspamguard.application.moderation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kspamguard.application.port.out.AuditLogPersistencePort;
import com.kspamguard.application.port.out.ModerationQueuePersistencePort;
import com.kspamguard.application.port.out.ModerationQueueQueryPort;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewModerationItemServiceTest {

  @Mock ModerationQueueQueryPort queryPort;
  @Mock ModerationQueuePersistencePort persistencePort;
  @Mock AuditLogPersistencePort auditLogPort;

  ReviewModerationItemService service;

  @BeforeEach
  void setUp() {
    service = new ReviewModerationItemService(queryPort, persistencePort, auditLogPort);
  }

  @Test
  void listPending_returnsPendingItems() {
    var items =
        List.of(
            new ModerationQueueItemView(1L, 10L, "HIDE", "PENDING_REVIEW", Instant.now()),
            new ModerationQueueItemView(2L, 11L, "REVIEW", "PENDING_REVIEW", Instant.now()));
    when(queryPort.findByStatus("PENDING_REVIEW")).thenReturn(items);

    var result = service.listPending();

    assertThat(result).hasSize(2);
    assertThat(result.get(0).id()).isEqualTo(1L);
    assertThat(result.get(1).recommendedAction()).isEqualTo("REVIEW");
  }

  @Test
  void approve_pendingItem_updatesToApprovedAndLogsAudit() {
    var item = new ModerationQueueItemView(1L, 10L, "HIDE", "PENDING_REVIEW", Instant.now());
    when(queryPort.findById(1L)).thenReturn(Optional.of(item));

    service.review(new ReviewModerationItemCommand(1L, ModerationAction.APPROVED, "명백한 스팸"));

    verify(persistencePort).review(eq(1L), eq("APPROVED"), any());
    verify(auditLogPort)
        .log(eq("moderation_approve"), eq("moderation_queue_item"), eq(1L), any(), any());
  }

  @Test
  void reject_pendingItem_updatesToRejectedAndLogsAudit() {
    var item = new ModerationQueueItemView(1L, 10L, "HIDE", "PENDING_REVIEW", Instant.now());
    when(queryPort.findById(1L)).thenReturn(Optional.of(item));

    service.review(new ReviewModerationItemCommand(1L, ModerationAction.REJECTED, null));

    verify(persistencePort).review(eq(1L), eq("REJECTED"), any());
    verify(auditLogPort)
        .log(eq("moderation_reject"), eq("moderation_queue_item"), eq(1L), eq(Map.of()), any());
  }

  @Test
  void approve_withReviewerNote_includesNoteInAuditLog() {
    var item = new ModerationQueueItemView(1L, 10L, "HIDE", "PENDING_REVIEW", Instant.now());
    when(queryPort.findById(1L)).thenReturn(Optional.of(item));

    service.review(new ReviewModerationItemCommand(1L, ModerationAction.APPROVED, "광고성 댓글"));

    ArgumentCaptor<Map<String, Object>> metaCaptor = ArgumentCaptor.forClass(Map.class);
    verify(auditLogPort)
        .log(
            eq("moderation_approve"),
            eq("moderation_queue_item"),
            eq(1L),
            metaCaptor.capture(),
            any());
    assertThat(metaCaptor.getValue()).containsEntry("reviewer_note", "광고성 댓글");
  }

  @Test
  void review_nonExistentItem_throwsNotFoundException() {
    when(queryPort.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(
            () ->
                service.review(
                    new ReviewModerationItemCommand(99L, ModerationAction.APPROVED, null)))
        .isInstanceOf(ModerationItemNotFoundException.class);

    verify(persistencePort, never()).review(any(), any(), any());
    verify(auditLogPort, never()).log(any(), any(), any(), any(), any());
  }

  @Test
  void review_alreadyReviewedItem_throwsAlreadyReviewedException() {
    var item = new ModerationQueueItemView(1L, 10L, "HIDE", "APPROVED", Instant.now());
    when(queryPort.findById(1L)).thenReturn(Optional.of(item));

    assertThatThrownBy(
            () ->
                service.review(
                    new ReviewModerationItemCommand(1L, ModerationAction.REJECTED, null)))
        .isInstanceOf(ModerationItemAlreadyReviewedException.class);

    verify(persistencePort, never()).review(any(), any(), any());
  }
}

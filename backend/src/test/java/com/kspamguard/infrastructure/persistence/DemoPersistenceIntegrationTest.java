package com.kspamguard.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.kspamguard.domain.detection.DetectionStatus;
import com.kspamguard.infrastructure.persistence.adapter.AuditLogPersistenceAdapter;
import com.kspamguard.infrastructure.persistence.adapter.CommentPersistenceAdapter;
import com.kspamguard.infrastructure.persistence.adapter.DetectionResultPersistenceAdapter;
import com.kspamguard.infrastructure.persistence.adapter.ModerationQueuePersistenceAdapter;
import com.kspamguard.infrastructure.persistence.adapter.ModerationQueueQueryAdapter;
import com.kspamguard.infrastructure.persistence.entity.AuditLogJpaEntity;
import com.kspamguard.infrastructure.persistence.entity.CommentJpaEntity;
import com.kspamguard.infrastructure.persistence.entity.DetectionResultJpaEntity;
import com.kspamguard.infrastructure.persistence.entity.ModerationQueueItemJpaEntity;
import com.kspamguard.infrastructure.persistence.repository.AuditLogJpaRepository;
import com.kspamguard.infrastructure.persistence.repository.CommentJpaRepository;
import com.kspamguard.infrastructure.persistence.repository.DetectionResultJpaRepository;
import com.kspamguard.infrastructure.persistence.repository.ModerationQueueItemJpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@Transactional
class DemoPersistenceIntegrationTest {

  @Container @ServiceConnection static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0");

  @Autowired CommentPersistenceAdapter commentAdapter;
  @Autowired DetectionResultPersistenceAdapter detectionResultAdapter;
  @Autowired ModerationQueuePersistenceAdapter moderationQueueAdapter;
  @Autowired ModerationQueueQueryAdapter moderationQueueQueryAdapter;
  @Autowired AuditLogPersistenceAdapter auditLogAdapter;
  @Autowired CommentJpaRepository commentRepo;
  @Autowired DetectionResultJpaRepository detectionResultRepo;
  @Autowired ModerationQueueItemJpaRepository moderationQueueRepo;
  @Autowired AuditLogJpaRepository auditLogRepo;

  @Test
  void saveComment_persistsAndReturnsId() {
    Long id = commentAdapter.save("demo-tc-001", "user1", "카지노 광고", "카지노 광고", Instant.now());

    assertThat(id).isNotNull();
    CommentJpaEntity saved = commentRepo.findById(id).orElseThrow();
    assertThat(saved.getExternalCommentId()).isEqualTo("demo-tc-001");
    assertThat(saved.getUsername()).isEqualTo("user1");
    assertThat(saved.getOriginalText()).isEqualTo("카지노 광고");
    assertThat(saved.getNormalizedText()).isEqualTo("카지노 광고");
  }

  @Test
  void saveDetectionResult_persistsLinkedToComment() {
    Long commentId = commentAdapter.save("demo-tc-002", "user2", "도박 사이트", "도박 사이트", Instant.now());

    detectionResultAdapter.save(
        commentId, DetectionStatus.SPAM, 92, List.of("GAMBLING_KEYWORD"), Instant.now());

    List<DetectionResultJpaEntity> results = detectionResultRepo.findAll();
    assertThat(results).hasSize(1);
    DetectionResultJpaEntity result = results.get(0);
    assertThat(result.getCommentId()).isEqualTo(commentId);
    assertThat(result.getStatus()).isEqualTo("SPAM");
    assertThat(result.getScore()).isEqualTo(92);
    assertThat(result.getReasonCodes()).containsExactly("GAMBLING_KEYWORD");
  }

  @Test
  void enqueueModerationItem_createsHideActionForSpam() {
    Long commentId = commentAdapter.save("demo-tc-003", "spammer", "스팸 댓글", "스팸 댓글", Instant.now());

    moderationQueueAdapter.enqueue(commentId, "HIDE", Instant.now());

    List<ModerationQueueItemJpaEntity> items = moderationQueueRepo.findAll();
    assertThat(items).hasSize(1);
    ModerationQueueItemJpaEntity item = items.get(0);
    assertThat(item.getCommentId()).isEqualTo(commentId);
    assertThat(item.getRecommendedAction()).isEqualTo("HIDE");
    assertThat(item.getStatus()).isEqualTo("PENDING_REVIEW");
  }

  @Test
  void safeComment_noModerationItemCreated() {
    Long commentId =
        commentAdapter.save("demo-tc-004", "user3", "좋은 영상이에요", "좋은 영상이에요", Instant.now());
    detectionResultAdapter.save(commentId, DetectionStatus.SAFE, 5, List.of(), Instant.now());

    assertThat(moderationQueueRepo.findAll()).isEmpty();
  }

  @Test
  void reasonCodes_serializedAsJsonAndRestoredCorrectly() {
    Long commentId = commentAdapter.save("demo-tc-005", "user5", "대출 카지노", "대출 카지노", Instant.now());
    detectionResultAdapter.save(
        commentId,
        DetectionStatus.SPAM,
        95,
        List.of("LOAN_KEYWORD", "GAMBLING_KEYWORD"),
        Instant.now());

    DetectionResultJpaEntity entity = detectionResultRepo.findAll().get(0);
    assertThat(entity.getReasonCodes())
        .containsExactlyInAnyOrder("LOAN_KEYWORD", "GAMBLING_KEYWORD");
  }

  @Test
  void reviewItem_approve_updatesStatusAndCreatesAuditLog() {
    Long commentId = commentAdapter.save("demo-tc-006", "spammer", "스팸", "스팸", Instant.now());
    moderationQueueAdapter.enqueue(commentId, "HIDE", Instant.now());
    Long itemId = moderationQueueRepo.findAll().get(0).getId();

    moderationQueueAdapter.review(itemId, "APPROVED", Instant.now());
    auditLogAdapter.log(
        "moderation_approve",
        "moderation_queue_item",
        itemId,
        Map.of("reviewer_note", "명백한 스팸"),
        Instant.now());

    ModerationQueueItemJpaEntity item = moderationQueueRepo.findById(itemId).orElseThrow();
    assertThat(item.getStatus()).isEqualTo("APPROVED");
    assertThat(item.getReviewedAt()).isNotNull();

    List<AuditLogJpaEntity> logs = auditLogRepo.findAll();
    assertThat(logs).hasSize(1);
    assertThat(logs.get(0).getEventType()).isEqualTo("moderation_approve");
    assertThat(logs.get(0).getTargetId()).isEqualTo(itemId);
  }

  @Test
  void reviewItem_reject_updatesStatusToRejected() {
    Long commentId = commentAdapter.save("demo-tc-007", "spammer2", "도박", "도박", Instant.now());
    moderationQueueAdapter.enqueue(commentId, "HIDE", Instant.now());
    Long itemId = moderationQueueRepo.findAll().get(0).getId();

    moderationQueueAdapter.review(itemId, "REJECTED", Instant.now());

    assertThat(moderationQueueRepo.findById(itemId).orElseThrow().getStatus())
        .isEqualTo("REJECTED");
  }

  @Test
  void findByStatus_returnsPendingItems() {
    Long c1 = commentAdapter.save("demo-tc-008a", "u1", "스팸1", "스팸1", Instant.now());
    Long c2 = commentAdapter.save("demo-tc-008b", "u2", "스팸2", "스팸2", Instant.now());
    moderationQueueAdapter.enqueue(c1, "HIDE", Instant.now());
    moderationQueueAdapter.enqueue(c2, "HIDE", Instant.now());

    var pending = moderationQueueQueryAdapter.findByStatus("PENDING_REVIEW");
    assertThat(pending).hasSize(2);
    assertThat(pending).allMatch(v -> "PENDING_REVIEW".equals(v.status()));
  }
}

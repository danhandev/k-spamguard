package com.kspamguard.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "moderation_queue_items")
public class ModerationQueueItemJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "comment_id", nullable = false)
  private Long commentId;

  @Column(name = "recommended_action", length = 20)
  private String recommendedAction;

  @Column(nullable = false, length = 30)
  private String status;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "reviewed_at")
  private Instant reviewedAt;

  @Column(name = "reviewer_note", length = 500)
  private String reviewerNote;

  protected ModerationQueueItemJpaEntity() {}

  public ModerationQueueItemJpaEntity(
      Long commentId, String recommendedAction, String status, Instant createdAt) {
    this.commentId = commentId;
    this.recommendedAction = recommendedAction;
    this.status = status;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public Long getCommentId() {
    return commentId;
  }

  public String getRecommendedAction() {
    return recommendedAction;
  }

  public String getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getReviewedAt() {
    return reviewedAt;
  }

  public void markReviewed(String newStatus, Instant reviewedAt) {
    this.status = newStatus;
    this.reviewedAt = reviewedAt;
  }
}

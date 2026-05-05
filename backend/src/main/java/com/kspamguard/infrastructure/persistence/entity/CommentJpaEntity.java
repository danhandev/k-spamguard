package com.kspamguard.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "comments")
public class CommentJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "external_comment_id", nullable = false, unique = true)
  private String externalCommentId;

  @Column(nullable = false)
  private String username;

  @Column(name = "original_text", nullable = false, columnDefinition = "TEXT")
  private String originalText;

  @Column(name = "normalized_text", columnDefinition = "TEXT")
  private String normalizedText;

  @Column(name = "received_at", nullable = false)
  private Instant receivedAt;

  protected CommentJpaEntity() {}

  public CommentJpaEntity(
      String externalCommentId,
      String username,
      String originalText,
      String normalizedText,
      Instant receivedAt) {
    this.externalCommentId = externalCommentId;
    this.username = username;
    this.originalText = originalText;
    this.normalizedText = normalizedText;
    this.receivedAt = receivedAt;
  }

  public Long getId() {
    return id;
  }

  public String getExternalCommentId() {
    return externalCommentId;
  }

  public String getUsername() {
    return username;
  }

  public String getOriginalText() {
    return originalText;
  }

  public String getNormalizedText() {
    return normalizedText;
  }

  public Instant getReceivedAt() {
    return receivedAt;
  }
}

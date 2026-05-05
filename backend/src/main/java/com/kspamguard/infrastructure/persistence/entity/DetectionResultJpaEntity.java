package com.kspamguard.infrastructure.persistence.entity;

import com.kspamguard.infrastructure.persistence.converter.StringListConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "detection_results")
public class DetectionResultJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "comment_id", nullable = false)
  private Long commentId;

  @Column(nullable = false, length = 20)
  private String status;

  @Column(nullable = false)
  private int score;

  @Column(name = "reason_codes", columnDefinition = "JSON")
  @Convert(converter = StringListConverter.class)
  private List<String> reasonCodes;

  @Column(name = "detected_at", nullable = false)
  private Instant detectedAt;

  protected DetectionResultJpaEntity() {}

  public DetectionResultJpaEntity(
      Long commentId, String status, int score, List<String> reasonCodes, Instant detectedAt) {
    this.commentId = commentId;
    this.status = status;
    this.score = score;
    this.reasonCodes = reasonCodes;
    this.detectedAt = detectedAt;
  }

  public Long getId() {
    return id;
  }

  public Long getCommentId() {
    return commentId;
  }

  public String getStatus() {
    return status;
  }

  public int getScore() {
    return score;
  }

  public List<String> getReasonCodes() {
    return reasonCodes;
  }

  public Instant getDetectedAt() {
    return detectedAt;
  }
}

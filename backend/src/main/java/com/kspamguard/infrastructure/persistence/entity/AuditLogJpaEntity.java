package com.kspamguard.infrastructure.persistence.entity;

import com.kspamguard.infrastructure.persistence.converter.JsonMapConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "audit_logs")
public class AuditLogJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "event_type", nullable = false, length = 50)
  private String eventType;

  @Column(name = "target_type", length = 50)
  private String targetType;

  @Column(name = "target_id")
  private Long targetId;

  @Column(name = "metadata_json", columnDefinition = "JSON")
  @Convert(converter = JsonMapConverter.class)
  private Map<String, Object> metadataJson;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected AuditLogJpaEntity() {}

  public AuditLogJpaEntity(
      String eventType,
      String targetType,
      Long targetId,
      Map<String, Object> metadataJson,
      Instant createdAt) {
    this.eventType = eventType;
    this.targetType = targetType;
    this.targetId = targetId;
    this.metadataJson = metadataJson;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public String getEventType() {
    return eventType;
  }

  public String getTargetType() {
    return targetType;
  }

  public Long getTargetId() {
    return targetId;
  }

  public Map<String, Object> getMetadataJson() {
    return metadataJson;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}

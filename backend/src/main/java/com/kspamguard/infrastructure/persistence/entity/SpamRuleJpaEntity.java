package com.kspamguard.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "spam_rules")
public class SpamRuleJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "rule_code", nullable = false, unique = true, length = 60)
  private String ruleCode;

  @Column(name = "rule_type", nullable = false, length = 20)
  private String ruleType;

  @Column(nullable = false, length = 500)
  private String pattern;

  @Column(nullable = false, precision = 4, scale = 2)
  private BigDecimal threshold;

  @Column(nullable = false)
  private boolean enabled;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected SpamRuleJpaEntity() {}

  public SpamRuleJpaEntity(
      String ruleCode,
      String ruleType,
      String pattern,
      BigDecimal threshold,
      boolean enabled,
      Instant createdAt,
      Instant updatedAt) {
    this.ruleCode = ruleCode;
    this.ruleType = ruleType;
    this.pattern = pattern;
    this.threshold = threshold;
    this.enabled = enabled;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public void applyUpdate(String pattern, BigDecimal threshold, boolean enabled, Instant updatedAt) {
    this.pattern = pattern;
    this.threshold = threshold;
    this.enabled = enabled;
    this.updatedAt = updatedAt;
  }

  public void disable(Instant updatedAt) {
    this.enabled = false;
    this.updatedAt = updatedAt;
  }

  public Long getId() {
    return id;
  }

  public String getRuleCode() {
    return ruleCode;
  }

  public String getRuleType() {
    return ruleType;
  }

  public String getPattern() {
    return pattern;
  }

  public BigDecimal getThreshold() {
    return threshold;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}

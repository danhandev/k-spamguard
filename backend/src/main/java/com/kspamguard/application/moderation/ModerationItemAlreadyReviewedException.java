package com.kspamguard.application.moderation;

public class ModerationItemAlreadyReviewedException extends RuntimeException {
  public ModerationItemAlreadyReviewedException(Long id, String currentStatus) {
    super("Moderation item " + id + " is already in status: " + currentStatus);
  }
}

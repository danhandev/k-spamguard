package com.kspamguard.application.moderation;

public class ModerationItemNotFoundException extends RuntimeException {
  public ModerationItemNotFoundException(Long id) {
    super("Moderation item not found: " + id);
  }
}

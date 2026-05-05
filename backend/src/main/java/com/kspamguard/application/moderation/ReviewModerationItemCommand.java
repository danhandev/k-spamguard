package com.kspamguard.application.moderation;

public record ReviewModerationItemCommand(Long id, ModerationAction action, String reviewerNote) {}

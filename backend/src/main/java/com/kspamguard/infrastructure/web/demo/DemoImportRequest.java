package com.kspamguard.infrastructure.web.demo;

import java.util.List;

public record DemoImportRequest(List<CommentEntry> comments) {

    public record CommentEntry(String externalCommentId, String username, String text) {
    }
}

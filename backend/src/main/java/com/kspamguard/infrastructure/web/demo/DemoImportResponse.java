package com.kspamguard.infrastructure.web.demo;

import java.util.List;

public record DemoImportResponse(int importedCount, List<ResultEntry> results) {

    public record ResultEntry(String externalCommentId, String status, int score, List<String> reasonCodes) {
    }
}

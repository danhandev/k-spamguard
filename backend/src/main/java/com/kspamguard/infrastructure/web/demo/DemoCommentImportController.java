package com.kspamguard.infrastructure.web.demo;

import com.kspamguard.application.demo.DemoCommentImportUseCase;
import com.kspamguard.application.demo.DemoCommentItem;
import com.kspamguard.application.demo.DemoDetectionResult;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo")
public class DemoCommentImportController {

  private final DemoCommentImportUseCase demoCommentImportUseCase;

  public DemoCommentImportController(DemoCommentImportUseCase demoCommentImportUseCase) {
    this.demoCommentImportUseCase = demoCommentImportUseCase;
  }

  @PostMapping("/comments/import")
  public ResponseEntity<DemoImportResponse> importComments(@RequestBody DemoImportRequest request) {
    List<DemoCommentItem> items =
        request.comments().stream()
            .map(c -> new DemoCommentItem(c.externalCommentId(), c.username(), c.text()))
            .toList();

    List<DemoDetectionResult> results = demoCommentImportUseCase.importAndDetect(items);

    List<DemoImportResponse.ResultEntry> entries =
        results.stream()
            .map(
                r ->
                    new DemoImportResponse.ResultEntry(
                        r.externalCommentId(), r.status().name(), r.score(), r.reasonCodes()))
            .toList();

    return ResponseEntity.ok(new DemoImportResponse(entries.size(), entries));
  }
}

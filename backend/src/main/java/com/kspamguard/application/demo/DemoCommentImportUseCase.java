package com.kspamguard.application.demo;

import java.util.List;

public interface DemoCommentImportUseCase {
  List<DemoDetectionResult> importAndDetect(List<DemoCommentItem> items);
}

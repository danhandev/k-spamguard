package com.kspamguard.infrastructure.web.demo;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.kspamguard.application.demo.DemoCommentImportUseCase;
import com.kspamguard.application.demo.DemoDetectionResult;
import com.kspamguard.domain.detection.DetectionStatus;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DemoCommentImportController.class)
class DemoCommentImportControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockitoBean private DemoCommentImportUseCase demoCommentImportUseCase;

  @Test
  void importComments_returns201WithSnakeCaseJson() throws Exception {
    when(demoCommentImportUseCase.importAndDetect(anyList()))
        .thenReturn(
            List.of(
                new DemoDetectionResult(
                    "demo_001", DetectionStatus.SPAM, 92, List.of("GAMBLING_KEYWORD"))));

    mockMvc
        .perform(
            post("/api/v1/demo/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "comments": [
                        {
                          "external_comment_id": "demo_001",
                          "username": "spammer",
                          "text": "카지노 무료머니"
                        }
                      ]
                    }
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.imported_count").value(1))
        .andExpect(jsonPath("$.results[0].external_comment_id").value("demo_001"))
        .andExpect(jsonPath("$.results[0].status").value("SPAM"))
        .andExpect(jsonPath("$.results[0].score").value(92))
        .andExpect(jsonPath("$.results[0].reason_codes[0]").value("GAMBLING_KEYWORD"));
  }

  @Test
  void importComments_emptyList_returns201WithZeroCount() throws Exception {
    when(demoCommentImportUseCase.importAndDetect(anyList())).thenReturn(List.of());

    mockMvc
        .perform(
            post("/api/v1/demo/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"comments": []}
                    """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.imported_count").value(0));
  }

  @Test
  void importComments_missingCommentsField_returns400() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/demo/comments").contentType(MediaType.APPLICATION_JSON).content("{}"))
        .andExpect(status().isBadRequest());
  }

  @Test
  void oldImportPath_returns404() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/demo/comments/import")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"comments": []}
                    """))
        .andExpect(status().isNotFound());
  }
}

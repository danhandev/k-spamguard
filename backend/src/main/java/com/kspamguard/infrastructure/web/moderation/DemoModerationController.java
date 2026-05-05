package com.kspamguard.infrastructure.web.moderation;

import com.kspamguard.application.moderation.ListModerationQueueUseCase;
import com.kspamguard.application.moderation.ModerationItemAlreadyReviewedException;
import com.kspamguard.application.moderation.ModerationItemNotFoundException;
import com.kspamguard.application.moderation.ReviewModerationItemCommand;
import com.kspamguard.application.moderation.ReviewModerationItemUseCase;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/demo/moderation")
public class DemoModerationController {

  private final ListModerationQueueUseCase listUseCase;
  private final ReviewModerationItemUseCase reviewUseCase;

  public DemoModerationController(
      ListModerationQueueUseCase listUseCase, ReviewModerationItemUseCase reviewUseCase) {
    this.listUseCase = listUseCase;
    this.reviewUseCase = reviewUseCase;
  }

  @GetMapping
  public List<ModerationItemResponse> listPending() {
    return listUseCase.listPending().stream().map(ModerationItemResponse::from).toList();
  }

  @PatchMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void review(@PathVariable Long id, @Valid @RequestBody ModerationReviewRequest request) {
    reviewUseCase.review(
        new ReviewModerationItemCommand(id, request.action(), request.reviewerNote()));
  }

  @ExceptionHandler(ModerationItemNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public void handleNotFound() {}

  @ExceptionHandler(ModerationItemAlreadyReviewedException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public void handleAlreadyReviewed() {}
}

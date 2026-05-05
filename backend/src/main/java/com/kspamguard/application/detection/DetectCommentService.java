package com.kspamguard.application.detection;

import com.kspamguard.domain.detection.Comment;
import com.kspamguard.domain.detection.DetectionResult;
import com.kspamguard.domain.detection.SpamDetector;
import org.springframework.stereotype.Service;

@Service
public class DetectCommentService implements DetectCommentUseCase {

    private final SpamDetector spamDetector;

    public DetectCommentService(SpamDetector spamDetector) {
        this.spamDetector = spamDetector;
    }

    @Override
    public DetectionResult detect(DetectCommentCommand command) {
        return spamDetector.detect(new Comment(command.text()), command.rules());
    }
}

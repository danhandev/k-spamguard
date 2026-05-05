package com.kspamguard.infrastructure.config;

import com.kspamguard.domain.detection.KoreanTextNormalizer;
import com.kspamguard.domain.detection.SpamDetectionEngine;
import com.kspamguard.domain.detection.SpamDetector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DetectionConfig {

    @Bean
    KoreanTextNormalizer koreanTextNormalizer() {
        return new KoreanTextNormalizer();
    }

    @Bean
    SpamDetector spamDetector(KoreanTextNormalizer normalizer) {
        return new SpamDetectionEngine(normalizer);
    }
}

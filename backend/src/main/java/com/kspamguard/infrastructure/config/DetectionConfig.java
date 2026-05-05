package com.kspamguard.infrastructure.config;

import com.kspamguard.domain.detection.KoreanNormalizer;
import com.kspamguard.domain.detection.RuleBasedSpamDetector;
import com.kspamguard.domain.detection.SpamDetector;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DetectionConfig {

    @Bean
    KoreanNormalizer koreanNormalizer() {
        return new KoreanNormalizer();
    }

    @Bean
    SpamDetector spamDetector(KoreanNormalizer normalizer) {
        return new RuleBasedSpamDetector(normalizer);
    }
}

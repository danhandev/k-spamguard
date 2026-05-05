package com.kspamguard.domain.detection;

import java.util.Locale;

public class KoreanNormalizer {

    public String normalize(String text) {
        if (text == null) return "";
        return text.trim()
                .replaceAll("(.)\\1{2,}", "$1$1")
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }
}

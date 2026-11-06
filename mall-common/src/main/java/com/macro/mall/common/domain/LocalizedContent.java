package com.macro.mall.common.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import java.io.Serializable;
import java.util.Map;

/** Chinese fields remain the canonical content; translations use the same field names. */
public class LocalizedContent implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(title = "多语言内容，例如 {\"pt\":{\"name\":\"Nome\"}}")
    private Map<String, Map<String, String>> translations;

    public Map<String, Map<String, String>> getTranslations() {
        return translations;
    }

    public void setTranslations(Map<String, Map<String, String>> translations) {
        this.translations = translations;
    }
}

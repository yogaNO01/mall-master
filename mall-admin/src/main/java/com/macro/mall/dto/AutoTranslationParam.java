package com.macro.mall.dto;

import lombok.Data;
import java.util.List;

@Data
public class AutoTranslationParam {
    private String text;
    private String sourceLang = "zh";
    private List<String> targetLangs = List.of("en", "pt", "es");
}

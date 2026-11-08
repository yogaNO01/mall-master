package com.macro.mall.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.macro.mall.common.api.CommonResult;
import com.macro.mall.dto.AutoTranslationParam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/translation")
@Tag(name = "Translation", description = "AI translation")
public class TranslationController {
    private static final Logger log = LoggerFactory.getLogger(TranslationController.class);
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    private final ObjectMapper mapper;
    private final OkHttpClient client;

    @Value("${translation.provider:google-web}") private String provider;
    @Value("${translation.deepl.api-key:}") private String deeplKey;
    @Value("${translation.deepl.base-url:https://api-free.deepl.com}") private String deeplUrl;
    private final Map<String, Map<String, String>> cache = java.util.Collections.synchronizedMap(new LinkedHashMap<>());
    @Value("${translation.dlx.url:http://127.0.0.1:1188}") private String dlxUrl;

    public TranslationController(ObjectMapper mapper,
            @Value("${translation.proxy.host:}") String proxyHost,
            @Value("${translation.proxy.port:0}") int proxyPort) {
        this.mapper = mapper;
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS);
        if (!proxyHost.isBlank() && proxyPort > 0) {
            builder.proxy(new java.net.Proxy(java.net.Proxy.Type.HTTP,
                    new java.net.InetSocketAddress(proxyHost, proxyPort)));
        }
        client = builder.build();
    }

    @PostMapping("/auto")
    @Operation(summary = "Translate Chinese content to English, Portuguese and Spanish")
    public CommonResult<Map<String, String>> auto(@org.springframework.web.bind.annotation.RequestBody AutoTranslationParam param) {
        if (param == null || param.getText() == null || param.getText().isBlank()) {
            return CommonResult.validateFailed("请输入中文内容");
        }
        try {
            if (param.getText().length() > 1500) return CommonResult.validateFailed("自动翻译单次最多1500字符，请分段填写");
            var targets = param.getTargetLangs() == null ? java.util.List.of("en", "pt", "es") : param.getTargetLangs();
            if (targets.isEmpty() || targets.stream().anyMatch(t -> !java.util.List.of("en", "pt", "es").contains(t))) return CommonResult.validateFailed("目标语言仅支持英文、葡萄牙语和西班牙语");
            String cacheKey = provider + targets + param.getText();
            Map<String, String> cached = cache.get(cacheKey);
            if (cached != null) return CommonResult.success(cached);
            Map<String, String> result = new LinkedHashMap<>();
            for (String target : targets) result.put(target, "deepl".equals(provider) ? translateDeepL(param.getText(), target) : translateGoogle(param.getText(), target));
            synchronized (cache) {
                if (cache.size() >= 1000) cache.remove(cache.keySet().iterator().next());
                cache.put(cacheKey, result);
            }
            return CommonResult.success(result);
        } catch (IOException e) {
            log.warn("Translation provider request failed: {}", e.getMessage(), e);
            return CommonResult.failed("翻译服务请求失败，请稍后重试");
        }
    }

    private String translateDeepL(String text, String target) throws IOException {
        if (deeplKey.isBlank()) throw new IOException("DeepL API Key 未配置");
        ObjectNode body = mapper.createObjectNode().put("source_lang", "ZH").put("target_lang", "pt".equals(target) ? "PT-BR" : "es".equals(target) ? "ES" : "EN");
        body.putArray("text").add(text);
        Request request = new Request.Builder().url(deeplUrl.replaceAll("/$", "") + "/v2/translate")
                .header("Authorization", "DeepL-Auth-Key " + deeplKey.trim())
                .post(RequestBody.create(mapper.writeValueAsBytes(body), JSON)).build();
        try (var response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) throw new IOException("DeepL HTTP " + response.code());
            String translated = mapper.readTree(response.body().bytes()).path("translations").path(0).path("text").asText("");
            if (translated.isBlank()) throw new IOException("DeepL 返回空翻译");
            return translated;
        }
    }

    private String translateGoogle(String text, String target) throws IOException {
        String url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=zh-CN&tl="
                + target + "&dt=t&q=" + java.net.URLEncoder.encode(text, java.nio.charset.StandardCharsets.UTF_8);
        Request request = new Request.Builder().url(url).get().build();
        try (var response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) throw new IOException("Translation unavailable");
            JsonNode segments = mapper.readTree(response.body().bytes()).path(0);
            StringBuilder result = new StringBuilder();
            for (JsonNode segment : segments) result.append(segment.path(0).asText(""));
            if (result.isEmpty()) throw new IOException("Empty translation");
            return result.toString();
        }
    }

    private String translateWithDlx(String text, String target) throws IOException {
        ObjectNode body = mapper.createObjectNode().put("text", text).put("source_lang", "ZH").put("target_lang", target);
        Request request = new Request.Builder().url(dlxUrl.replaceAll("/$", "") + "/translate")
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(mapper.writeValueAsBytes(body), JSON)).build();
        try (var response = client.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) throw new IOException("DLX unavailable");
            JsonNode root = mapper.readTree(response.body().bytes());
            String translated = root.path("data").asText("");
            if (translated.isBlank()) throw new IOException("DLX returned empty translation");
            return translated;
        }
    }
}

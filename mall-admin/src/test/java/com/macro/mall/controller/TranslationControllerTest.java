package com.macro.mall.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TranslationControllerTest {
    @Test
    void translatesChineseToSpanishWithDeepLTargetCode() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v2/translate", exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] response = "{\"translations\":[{\"text\":\"Zapatos\"}]}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (var body = exchange.getResponseBody()) {
                body.write(response);
            }
        });
        server.start();
        try {
            TranslationController controller = new TranslationController(new ObjectMapper(), "", 0);
            ReflectionTestUtils.setField(controller, "provider", "deepl");
            ReflectionTestUtils.setField(controller, "deeplKey", "test-key");
            ReflectionTestUtils.setField(controller, "deeplUrl", "http://127.0.0.1:" + server.getAddress().getPort());
            MockMvcBuilders.standaloneSetup(controller).build()
                    .perform(post("/translation/auto")
                            .contentType("application/json")
                            .content("{\"text\":\"鞋子\",\"targetLangs\":[\"es\"]}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(200))
                    .andExpect(jsonPath("$.data.es").value("Zapatos"));
            assertEquals("ES", new ObjectMapper().readTree(requestBody.get()).path("target_lang").asText());
        } finally {
            server.stop(0);
        }
    }
}

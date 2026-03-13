package com.ljy.xx_mangaer_system.service.mcp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ljy.xx_mangaer_system.config.mcp.McpProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpModelService {

    private final McpProperties mcpProperties;
    private final ObjectMapper objectMapper;

    private WebClient webClient;

    @PostConstruct
    public void init() {
        McpProperties.DeepSeekR1 modelConfig = mcpProperties.getModel().getDeepseekR1();
        this.webClient = WebClient.builder()
                .baseUrl(modelConfig.getApiUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + modelConfig.getApiKey())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
        log.info("MCP Model Service initialized with model: {}", modelConfig.getModelName());
    }

    /**
     * 调用DeepSeek R1模型
     */
    public Mono<ModelResponse> chatCompletion(List<McpContextService.ModelMessage> messages) {
        McpProperties.DeepSeekR1 config = mcpProperties.getModel().getDeepseekR1();

        ChatRequest request = new ChatRequest();
        request.setModel(config.getModelName());
        request.setMessages(messages);
        request.setTemperature(config.getTemperature());
        request.setMax_tokens(config.getMaxTokens());

        log.debug("Sending request to MCP model: {}", config.getModelName());

        return webClient.post()
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .map(this::parseResponse)
                .doOnNext(response -> log.debug("Received response from MCP model"))
                .doOnError(error -> log.error("Error calling MCP model: {}", error.getMessage()));
    }

    /**
     * 解析API响应
     */
    private ModelResponse parseResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            ModelResponse response = new ModelResponse();

            // 解析choices
            if (root.has("choices") && root.get("choices").isArray() && root.get("choices").size() > 0) {
                JsonNode choice = root.get("choices").get(0);
                if (choice.has("message")) {
                    JsonNode message = choice.get("message");
                    response.setContent(message.get("content").asText());
                    response.setRole(message.get("role").asText());
                }
            }

            // 解析usage
            if (root.has("usage")) {
                JsonNode usage = root.get("usage");
                response.setPromptTokens(usage.get("prompt_tokens").asInt());
                response.setCompletionTokens(usage.get("completion_tokens").asInt());
                response.setTotalTokens(usage.get("total_tokens").asInt());
            }

            return response;
        } catch (Exception e) {
            log.error("Failed to parse MCP response: {}", e.getMessage());
            throw new RuntimeException("Failed to parse model response", e);
        }
    }

    /**
     * 请求实体
     */
    @Data
    public static class ChatRequest {
        private String model;
        private List<McpContextService.ModelMessage> messages;
        private double temperature;
        private int max_tokens;
    }

    /**
     * 响应实体
     */
    @Data
    public static class ModelResponse {
        private String content;
        private String role;
        private int promptTokens;
        private int completionTokens;
        private int totalTokens;
    }
}

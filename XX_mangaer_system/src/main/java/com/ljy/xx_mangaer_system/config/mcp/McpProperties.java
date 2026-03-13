package com.ljy.xx_mangaer_system.config.mcp;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "mcp")
public class McpProperties {

    private Context context;
    private Model model;

    @Data
    public static class Context {
        private int maxHistory;
        private int expireMinutes;
    }

    @Data
    public static class Model {
        private DeepSeekR1 deepseekR1;
    }

    @Data
    public static class DeepSeekR1 {
        private String apiUrl;
        private String apiKey;
        private String modelName;
        private double temperature;
        private int maxTokens;
    }
}

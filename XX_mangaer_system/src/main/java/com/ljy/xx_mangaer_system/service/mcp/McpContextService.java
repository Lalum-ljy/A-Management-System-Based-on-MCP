package com.ljy.xx_mangaer_system.service.mcp;

import com.ljy.xx_mangaer_system.config.mcp.McpProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpContextService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final McpProperties mcpProperties;

    private static final String MCP_CONTEXT_KEY_PREFIX = "mcp:context:";
    
    // 内存备选存储（Redis不可用时使用）
    private final Map<String, List<ContextMessage>> memoryContext = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("MCP Context Service initialized with max-history: {}, expire-minutes: {}",
                mcpProperties.getContext().getMaxHistory(),
                mcpProperties.getContext().getExpireMinutes());
    }

    /**
     * 获取用户上下文
     */
    public List<ContextMessage> getContext(String userId) {
        try {
            String key = MCP_CONTEXT_KEY_PREFIX + userId;
            List<ContextMessage> context = (List<ContextMessage>) redisTemplate.opsForValue().get(key);
            return context != null ? context : new ArrayList<>();
        } catch (Exception e) {
            log.warn("Redis connection failed, using memory context for user: {}", userId);
            return memoryContext.getOrDefault(userId, new ArrayList<>());
        }
    }

    /**
     * 添加消息到上下文
     */
    public void addMessage(String userId, String role, String content) {
        String key = MCP_CONTEXT_KEY_PREFIX + userId;
        List<ContextMessage> context = getContext(userId);

        // 添加新消息
        context.add(new ContextMessage(role, content, System.currentTimeMillis()));

        // 限制上下文长度
        int maxHistory = mcpProperties.getContext().getMaxHistory();
        if (context.size() > maxHistory) {
            context = context.subList(context.size() - maxHistory, context.size());
        }

        // 保存到Redis并设置过期时间
        try {
            int expireMinutes = mcpProperties.getContext().getExpireMinutes();
            redisTemplate.opsForValue().set(key, context, expireMinutes, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("Redis connection failed, saving to memory for user: {}", userId);
            memoryContext.put(userId, context);
        }
    }

    /**
     * 清除用户上下文
     */
    public void clearContext(String userId) {
        try {
            String key = MCP_CONTEXT_KEY_PREFIX + userId;
            redisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis connection failed, clearing memory context for user: {}", userId);
        }
        memoryContext.remove(userId);
        log.info("Cleared MCP context for user: {}", userId);
    }

    /**
     * 构建模型请求的messages数组
     */
    public List<ModelMessage> buildMessages(String userId, String currentMessage) {
        List<ContextMessage> context = getContext(userId);
        List<ModelMessage> messages = new ArrayList<>();

        // 添加系统提示
        String systemPrompt = "你是一个命令转换助手，负责将用户的口语化语句转换为特定格式的指令。\n" +
                "指令格式：/command param1=value1 param2=value2\n" +
                "支持的命令：\n" +
                "- add-user: 添加增加用户，参数：username, password\n" +
                "- delete-user: 删除用户，参数：id\n" +
                "- update-user: 更新用户，参数：id, username, password\n" +
                "- list-users: 给出所有用户\n" +
                "- get-user: 获取用户信息，参数：id\n" +
                "请根据用户的输入内容，判断用户的意图，并转换为对应的指令格式。\n" +
                "只返回转换后的指令，不要添加任何其他内容。";
        messages.add(new ModelMessage("system", systemPrompt));

        // 添加历史上下文
        for (ContextMessage msg : context) {
            messages.add(new ModelMessage(msg.getRole(), msg.getContent()));
        }

        // 添加当前消息
        messages.add(new ModelMessage("user", currentMessage));

        return messages;
    }

    /**
     * 上下文消息实体
     */
    @Data
    public static class ContextMessage {
        private String role;
        private String content;
        private long timestamp;

        public ContextMessage(String role, String content, long timestamp) {
            this.role = role;
            this.content = content;
            this.timestamp = timestamp;
        }
    }

    /**
     * 模型消息格式
     */
    @Data
    public static class ModelMessage {
        private String role;
        private String content;

        public ModelMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }
    }
}

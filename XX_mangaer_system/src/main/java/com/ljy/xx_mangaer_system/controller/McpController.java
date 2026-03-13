package com.ljy.xx_mangaer_system.controller;

import com.ljy.xx_mangaer_system.service.mcp.McpCommandParserService;
import com.ljy.xx_mangaer_system.service.mcp.McpContextService;
import com.ljy.xx_mangaer_system.service.mcp.McpModelService;
import com.ljy.xx_mangaer_system.service.mcp.McpUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/mcp")
@RequiredArgsConstructor
@Tag(name = "MCP协议接口", description = "Model Context Protocol标准化接口")
public class McpController {

    private final McpContextService contextService;
    private final McpModelService modelService;
    private final McpCommandParserService commandParserService;
    private final McpUserService userService;

    /**
     * MCP对话接口（带上下文管理）
     */
    @PostMapping("/chat")
    @Operation(summary = "MCP对话", description = "发送消息给AI模型，自动管理上下文")
    public Mono<ResponseEntity<Map<String, Object>>> chat(
            @Parameter(description = "用户ID（不传则自动生成）") @RequestHeader(value = "X-User-Id", required = false) String userId,
            @Parameter(description = "对话请求") @RequestBody ChatRequest request) {

        // 生成用户ID（如果没有提供）
        if (userId == null || userId.isEmpty()) {
            userId = UUID.randomUUID().toString();
        }

        final String finalUserId = userId;

        log.info("MCP chat request from user: {}, message: {}", finalUserId, request.getMessage());

        // 检查是否是指令
        if (commandParserService.isCommand(request.getMessage())) {
            return Mono.just(handleCommand(finalUserId, request.getMessage()));
        }

        // 构建包含上下文的messages
        var messages = contextService.buildMessages(finalUserId, request.getMessage());

        // 调用模型
        return modelService.chatCompletion(messages)
                .map(response -> {
                    // 保存对话上下文
                    contextService.addMessage(finalUserId, "user", request.getMessage());
                    contextService.addMessage(finalUserId, "assistant", response.getContent());

                    // 检查模型返回的是否是指令
                    String modelResponse = response.getContent().trim();
                    if (commandParserService.isCommand(modelResponse)) {
                        // 执行指令
                        log.info("Model returned command: {}", modelResponse);
                        return handleCommand(finalUserId, modelResponse);
                    }

                    // 构建响应
                    Map<String, Object> result = new HashMap<>();
                    result.put("code", 200);
                    result.put("message", "success");

                    Map<String, Object> data = new HashMap<>();
                    data.put("content", modelResponse);
                    data.put("userId", finalUserId);
                    data.put("usage", Map.of(
                            "promptTokens", response.getPromptTokens(),
                            "completionTokens", response.getCompletionTokens(),
                            "totalTokens", response.getTotalTokens()
                    ));

                    result.put("data", data);

                    return ResponseEntity.ok(result);
                })
                .onErrorResume(error -> {
                    log.error("MCP chat error: {}", error.getMessage());
                    Map<String, Object> errorResult = new HashMap<>();
                    errorResult.put("code", 500);
                    errorResult.put("message", "模型调用失败: " + error.getMessage());
                    return Mono.just(ResponseEntity.internalServerError().body(errorResult));
                });
    }

    /**
     * 处理指令
     */
    private ResponseEntity<Map<String, Object>> handleCommand(String userId, String message) {
        var command = commandParserService.parseCommand(message);
        if (command == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 400);
            result.put("message", "指令格式错误，请检查指令格式");
            return ResponseEntity.badRequest().body(result);
        }

        McpUserService.OperationResult operationResult;
        switch (command.getType()) {
            case "add-user":
                operationResult = userService.addUser(
                        command.getParams().get("username"),
                        command.getParams().get("password")
                );
                break;
            case "delete-user":
                try {
                    Long id = Long.parseLong(command.getParams().get("id"));
                    operationResult = userService.deleteUser(id);
                } catch (NumberFormatException e) {
                    operationResult = new McpUserService.OperationResult(false, "用户ID格式错误");
                }
                break;
            case "update-user":
                try {
                    Long id = Long.parseLong(command.getParams().get("id"));
                    operationResult = userService.updateUser(
                            id,
                            command.getParams().get("username"),
                            command.getParams().get("password")
                    );
                } catch (NumberFormatException e) {
                    operationResult = new McpUserService.OperationResult(false, "用户ID格式错误");
                }
                break;
            case "list-users":
                operationResult = userService.listUsers();
                break;
            case "get-user":
                try {
                    Long id = Long.parseLong(command.getParams().get("id"));
                    operationResult = userService.getUserById(id);
                } catch (NumberFormatException e) {
                    operationResult = new McpUserService.OperationResult(false, "用户ID格式错误");
                }
                break;
            default:
                operationResult = new McpUserService.OperationResult(false, "未知指令: " + command.getType());
        }

        // 构建响应
        Map<String, Object> result = new HashMap<>();
        result.put("code", operationResult.isSuccess() ? 200 : 400);
        
        // 处理口语化响应
        if (operationResult.isSuccess() && operationResult.getData() != null) {
            if (command.getType().equals("list-users")) {
                // 处理用户列表
                List<com.ljy.xx_mangaer_system.entity.User> users = (List<com.ljy.xx_mangaer_system.entity.User>) operationResult.getData();
                StringBuilder sb = new StringBuilder("用户列表如下：\n");
                for (int i = 0; i < users.size(); i++) {
                    com.ljy.xx_mangaer_system.entity.User user = users.get(i);
                    sb.append(i + 1).append(". 用户ID: " + user.getId() + ", 用户名: " + user.getUsername() + "\n");
                }
                result.put("message", sb.toString());
            } else if (command.getType().equals("get-user")) {
                // 处理单个用户
                com.ljy.xx_mangaer_system.entity.User user = (com.ljy.xx_mangaer_system.entity.User) operationResult.getData();
                String userInfo = "用户信息：\n" +
                        "ID: " + user.getId() + "\n" +
                        "用户名: " + user.getUsername();
                result.put("message", userInfo);
            } else {
                // 其他操作
                result.put("message", operationResult.getMessage());
            }
        } else {
            result.put("message", operationResult.getMessage());
        }

        return ResponseEntity.ok(result);
    }

    /**
     * 清除用户上下文
     */
    @PostMapping("/clear-context")
    @Operation(summary = "清除上下文", description = "清除指定用户的对话上下文")
    public ResponseEntity<Map<String, Object>> clearContext(
            @Parameter(description = "用户ID") @RequestHeader("X-User-Id") String userId) {

        contextService.clearContext(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "上下文已清除");

        return ResponseEntity.ok(result);
    }

    /**
     * 获取用户上下文（调试用）
     */
    @GetMapping("/context")
    @Operation(summary = "获取上下文", description = "获取指定用户的对话上下文（调试用）")
    public ResponseEntity<Map<String, Object>> getContext(
            @Parameter(description = "用户ID") @RequestHeader("X-User-Id") String userId) {

        var context = contextService.getContext(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "success");
        result.put("data", context);

        return ResponseEntity.ok(result);
    }

    /**
     * 对话请求实体
     */
    @Data
    public static class ChatRequest {
        private String message;
    }
}

package com.ljy.xx_mangaer_system.service.mcp;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class McpCommandParserService {

    /**
     * 解析指令
     */
    public Command parseCommand(String command) {
        log.debug("Parsing command: {}", command);
        
        String[] parts = command.trim().split("\\s+");
        if (parts.length == 0 || !parts[0].startsWith("/")) {
            log.debug("Not a command: {}", command);
            return null;
        }

        Command result = new Command();
        result.setType(parts[0].substring(1).toLowerCase());
        result.setParams(new HashMap<>());

        // 解析参数
        for (int i = 1; i < parts.length; i++) {
            String part = parts[i];
            int equalsIndex = part.indexOf('=');
            if (equalsIndex > 0) {
                String key = part.substring(0, equalsIndex).toLowerCase();
                String value = part.substring(equalsIndex + 1);
                result.getParams().put(key, value);
                log.debug("Parsed param: key={}, value={}", key, value);
            }
        }

        log.debug("Parsed command: type={}, params={}", result.getType(), result.getParams());
        return result;
    }

    /**
     * 检查是否是指令
     */
    public boolean isCommand(String input) {
        return input != null && input.trim().startsWith("/");
    }

    @Data
    public static class Command {
        private String type;
        private Map<String, String> params;
    }
}

package com.ljy.xx_mangaer_system.service.mcp;

import com.ljy.xx_mangaer_system.entity.User;
import com.ljy.xx_mangaer_system.service.UserService;
import com.ljy.xx_mangaer_system.utils.PasswordUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class McpUserService {

    private final UserService userService;

    /**
     * 添加用户
     */
    public OperationResult addUser(String username, String password) {
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            return new OperationResult(false, "用户名和密码不能为空");
        }

        try {
            User user = new User();
            user.setUsername(username);
            user.setPassword(PasswordUtils.encryptPassword(password));
            
            boolean success = userService.save(user);
            if (success) {
                return new OperationResult(true, "用户添加成功", user);
            } else {
                return new OperationResult(false, "用户添加失败");
            }
        } catch (Exception e) {
            log.error("Add user error: {}", e.getMessage());
            return new OperationResult(false, "添加用户时发生错误: " + e.getMessage());
        }
    }

    /**
     * 删除用户
     */
    public OperationResult deleteUser(Long id) {
        if (id == null) {
            return new OperationResult(false, "用户ID不能为空");
        }

        try {
            boolean success = userService.removeById(id);
            if (success) {
                return new OperationResult(true, "用户删除成功");
            } else {
                return new OperationResult(false, "用户删除失败，可能用户不存在");
            }
        } catch (Exception e) {
            log.error("Delete user error: {}", e.getMessage());
            return new OperationResult(false, "删除用户时发生错误: " + e.getMessage());
        }
    }

    /**
     * 更新用户
     */
    public OperationResult updateUser(Long id, String username, String password) {
        if (id == null) {
            return new OperationResult(false, "用户ID不能为空");
        }

        try {
            User user = userService.getById(id);
            if (user == null) {
                return new OperationResult(false, "用户不存在");
            }

            if (username != null && !username.isEmpty()) {
                user.setUsername(username);
            }
            if (password != null && !password.isEmpty()) {
                user.setPassword(PasswordUtils.encryptPassword(password));
            }

            boolean success = userService.updateById(user);
            if (success) {
                return new OperationResult(true, "用户更新成功", user);
            } else {
                return new OperationResult(false, "用户更新失败");
            }
        } catch (Exception e) {
            log.error("Update user error: {}", e.getMessage());
            return new OperationResult(false, "更新用户时发生错误: " + e.getMessage());
        }
    }

    /**
     * 查询用户列表
     */
    public OperationResult listUsers() {
        try {
            List<User> users = userService.list();
            return new OperationResult(true, "查询成功", users);
        } catch (Exception e) {
            log.error("List users error: {}", e.getMessage());
            return new OperationResult(false, "查询用户列表时发生错误: " + e.getMessage());
        }
    }

    /**
     * 根据ID查询用户
     */
    public OperationResult getUserById(Long id) {
        if (id == null) {
            return new OperationResult(false, "用户ID不能为空");
        }

        try {
            User user = userService.getById(id);
            if (user != null) {
                return new OperationResult(true, "查询成功", user);
            } else {
                return new OperationResult(false, "用户不存在");
            }
        } catch (Exception e) {
            log.error("Get user error: {}", e.getMessage());
            return new OperationResult(false, "查询用户时发生错误: " + e.getMessage());
        }
    }

    @Data
    public static class OperationResult {
        private boolean success;
        private String message;
        private Object data;

        public OperationResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        public OperationResult(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }
    }
}

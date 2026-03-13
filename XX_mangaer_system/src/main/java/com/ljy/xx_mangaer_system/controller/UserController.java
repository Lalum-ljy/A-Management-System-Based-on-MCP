package com.ljy.xx_mangaer_system.controller;

import com.ljy.xx_mangaer_system.entity.User;
import com.ljy.xx_mangaer_system.service.UserService;
import com.ljy.xx_mangaer_system.utils.PasswordUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/user")
@Tag(name = "用户管理", description = "用户相关API")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @GetMapping("/list")
    @Operation(summary = "获取用户列表", description = "获取所有用户信息")
    public Map<String, Object> list() {
        List<User> list = userService.list();
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "查询成功");
        result.put("data", list);
        return result;
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取用户", description = "根据用户ID获取用户详细信息")
    public Map<String, Object> getById(@Parameter(description = "用户ID") @PathVariable Long id) {
        User user = userService.getById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "查询成功");
        result.put("data", user);
        return result;
    }

    @PostMapping("/add")
    @Operation(summary = "添加用户", description = "添加新用户（密码会自动加密）")
    public Map<String, Object> add(@Parameter(description = "用户信息") @RequestBody User user) {
        // 对密码进行加密
        user.setPassword(PasswordUtils.encryptPassword(user.getPassword()));
        boolean success = userService.save(user);
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("message", success ? "添加成功" : "添加失败");
        return result;
    }

    @PutMapping("/update")
    @Operation(summary = "更新用户", description = "更新用户信息（密码会自动加密）")
    public Map<String, Object> update(@Parameter(description = "用户信息") @RequestBody User user) {
        // 对密码进行加密
        user.setPassword(PasswordUtils.encryptPassword(user.getPassword()));
        boolean success = userService.updateById(user);
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("message", success ? "更新成功" : "更新失败");
        return result;
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "根据用户ID删除用户")
    public Map<String, Object> delete(@Parameter(description = "用户ID") @PathVariable Long id) {
        boolean success = userService.removeById(id);
        Map<String, Object> result = new HashMap<>();
        result.put("code", success ? 200 : 500);
        result.put("message", success ? "删除成功" : "删除失败");
        return result;
    }

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户登录并获取token")
    public Map<String, Object> login(@Parameter(description = "登录信息") @RequestBody Map<String, String> loginInfo) {
        String username = loginInfo.get("username");
        String password = loginInfo.get("password");

        // 根据用户名查询用户
        User user = userService.getByUsername(username);
        if (user == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 401);
            result.put("message", "用户名或密码错误");
            return result;
        }

        // 验证密码
        if (!PasswordUtils.matches(password, user.getPassword())) {
            Map<String, Object> result = new HashMap<>();
            result.put("code", 401);
            result.put("message", "用户名或密码错误");
            return result;
        }

        // 生成token
        String token = UUID.randomUUID().toString();

        // 尝试将用户信息存储到Redis
        try {
            // 将用户信息存储到Redis，设置过期时间为1小时
            redisTemplate.opsForValue().set("user:token:" + token, user, 1, TimeUnit.HOURS);
        } catch (Exception e) {
            // Redis连接失败，记录错误但不影响登录
            System.out.println("Redis connection failed: " + e.getMessage());
        }

        // 返回登录结果和token
        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "登录成功");
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        result.put("data", data);
        return result;
    }

    @PostMapping("/logout")
    @Operation(summary = "用户登出", description = "用户登出并清除token")
    public Map<String, Object> logout(@Parameter(description = "token") @RequestHeader("token") String token) {
        // 尝试从Redis中删除token
        try {
            // 从Redis中删除token
            redisTemplate.delete("user:token:" + token);
        } catch (Exception e) {
            // Redis连接失败，记录错误但不影响登出
            System.out.println("Redis connection failed: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("code", 200);
        result.put("message", "登出成功");
        return result;
    }
}

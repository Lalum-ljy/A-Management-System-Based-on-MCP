package com.ljy.xx_mangaer_system.runner;

import com.ljy.xx_mangaer_system.entity.User;
import com.ljy.xx_mangaer_system.service.UserService;
import com.ljy.xx_mangaer_system.utils.PasswordUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PasswordUpdateRunner implements CommandLineRunner {

    @Autowired
    private UserService userService;

    @Override
    public void run(String... args) throws Exception {
        // 查询所有用户
        List<User> users = userService.list();
        
        // 遍历用户列表，更新密码为哈希加密后的值
        for (User user : users) {
            // 检查密码是否已经是加密的（BCrypt加密的密码长度通常大于60）
            if (user.getPassword().length() < 60) {
                // 对密码进行加密
                String encryptedPassword = PasswordUtils.encryptPassword(user.getPassword());
                user.setPassword(encryptedPassword);
                // 更新用户信息
                userService.updateById(user);
                System.out.println("Updated password for user: " + user.getUsername());
            }
        }
        
        System.out.println("Password update completed!");
    }
}

package com.ljy.xx_mangaer_system.utils;

import com.ljy.xx_mangaer_system.entity.User;
import com.ljy.xx_mangaer_system.service.UserService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.List;

@Configuration
@ComponentScan(basePackages = "com.ljy.xx_mangaer_system")
@EnableTransactionManagement
@EnableRedisRepositories
public class PasswordUpdateUtil {

    public static void main(String[] args) {
        // Create Spring application context
        ApplicationContext context = new AnnotationConfigApplicationContext(PasswordUpdateUtil.class);
        
        // Get UserService bean
        UserService userService = context.getBean(UserService.class);
        
        try {
            // Query all users
            List<User> users = userService.list();
            
            // Traverse user list, update password to hashed value
            for (User user : users) {
                // Check if password is already encrypted (BCrypt encrypted password length is usually greater than 60)
                if (user.getPassword().length() < 60) {
                    // Encrypt password
                    String encryptedPassword = PasswordUtils.encryptPassword(user.getPassword());
                    user.setPassword(encryptedPassword);
                    // Update user information
                    userService.updateById(user);
                    System.out.println("Updated password for user: " + user.getUsername());
                }
            }
            
            System.out.println("Password update completed!");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Close application context
            ((AnnotationConfigApplicationContext) context).close();
        }
    }
}

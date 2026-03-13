package com.ljy.xx_mangaer_system.utils;

import java.sql.*;

public class SimplePasswordUpdateUtil {

    public static void main(String[] args) {
        // 数据库连接信息
        String url = "jdbc:mysql://localhost:3306/manage?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai";
        String username = "root";
        String password = "123456";
        
        Connection conn = null;
        PreparedStatement stmt = null;
        ResultSet rs = null;
        
        try {
            // 加载数据库驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // 建立数据库连接
            conn = DriverManager.getConnection(url, username, password);
            
            // 查询所有用户
            String querySql = "SELECT id, username, password FROM user";
            stmt = conn.prepareStatement(querySql);
            rs = stmt.executeQuery();
            
            // 遍历结果集，更新密码
            while (rs.next()) {
                long id = rs.getLong("id");
                String user = rs.getString("username");
                String pwd = rs.getString("password");
                
                // 检查密码是否已经是加密的（BCrypt加密的密码长度通常大于60）
                if (pwd.length() < 60) {
                    // 对密码进行加密
                    String encryptedPassword = PasswordUtils.encryptPassword(pwd);
                    
                    // 更新密码
                    String updateSql = "UPDATE user SET password = ? WHERE id = ?";
                    PreparedStatement updateStmt = conn.prepareStatement(updateSql);
                    updateStmt.setString(1, encryptedPassword);
                    updateStmt.setLong(2, id);
                    updateStmt.executeUpdate();
                    updateStmt.close();
                    
                    System.out.println("Updated password for user: " + user);
                }
            }
            
            System.out.println("Password update completed!");
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 关闭资源
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }
}

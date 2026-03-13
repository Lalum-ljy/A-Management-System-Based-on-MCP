package com.ljy.xx_mangaer_system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ljy.xx_mangaer_system.entity.User;

public interface UserService extends IService<User> {
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    User getByUsername(String username);
}

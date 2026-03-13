package com.ljy.xx_mangaer_system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@TableName("user")
@Schema(name = "User", description = "用户实体")
public class User {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "用户ID", example = "1")
    private Long id;

    @TableField("username")
    @Schema(description = "用户名", example = "admin")
    private String username;

    @TableField("password")
    @Schema(description = "密码", example = "123456")
    private String password;
}

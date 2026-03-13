package com.ljy.xx_mangaer_system;

import com.ljy.xx_mangaer_system.config.mcp.McpProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@MapperScan("com.ljy.xx_mangaer_system.mapper")
@EnableConfigurationProperties(McpProperties.class)
public class XxMangaerSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(XxMangaerSystemApplication.class, args);
    }

}

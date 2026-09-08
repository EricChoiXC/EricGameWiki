package com.wiki;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * EricGameWiki 后端服务启动入口。
 * <p>
 * 扫描 com.wiki 全部包；MyBatis Mapper 扫描各模块 {@code model.mapper} 包。
 *
 * @author Eric
 * @date 2026/9/8
 */
@SpringBootApplication
@MapperScan("com.wiki.**.model.mapper")
public class WikiServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WikiServiceApplication.class, args);
    }
}

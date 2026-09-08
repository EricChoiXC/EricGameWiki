package com.wiki.admin.sys.common.service.impl;

import com.wiki.admin.sys.common.service.ICommonSettingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 公共服务-配置表 启动同步器，对应 {@code docs/admin/公共服务.md} 业务第 1 条。
 * <p>
 * 优先于 org 模块启动（org 创建默认用户需要读取默认密码配置）。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class CommonSettingStartupRunner implements CommandLineRunner {

    private final ICommonSettingService commonSettingService;

    @Override
    public void run(String... args) {
        log.info("[公共服务] 开始同步 setting.yml 配置项");
        commonSettingService.syncSettingsFromYml();
        log.info("[公共服务] setting.yml 配置项同步完成");
    }
}

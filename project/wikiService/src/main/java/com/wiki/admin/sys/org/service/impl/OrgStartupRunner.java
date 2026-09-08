package com.wiki.admin.sys.org.service.impl;

import com.wiki.admin.sys.org.service.IOrgAuthService;
import com.wiki.admin.sys.org.service.IOrgRoleService;
import com.wiki.admin.sys.org.service.IOrgUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 用户与权限模块启动初始化器，对应 {@code docs/admin/用户和权限管理.md} 业务第 1-3 条：
 * <ol>
 *   <li>检查并新建默认 admin 用户</li>
 *   <li>读取所有模块的 roles.yml，增量保存权限到 admin_org_role 表</li>
 *   <li>检查并新建默认系统管理员角色，赋值所有权限与 admin 用户</li>
 * </ol>
 * <p>
 * 顺序：在公共服务 setting 同步（Order=1）之后执行，确保 admin 用户创建时能读取默认密码配置。
 *
 * @author Eric
 * @date 2026/9/8
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class OrgStartupRunner implements CommandLineRunner {

    private final IOrgUserService userService;
    private final IOrgRoleService roleService;
    private final IOrgAuthService authService;

    @Override
    public void run(String... args) {
        log.info("[用户与权限] 开始启动初始化");

        // 业务第 1 条：检查并新建默认 admin 用户
        userService.ensureDefaultAdminUser();

        // 业务第 2 条：读取所有模块的 roles.yml，增量保存权限到 admin_org_role 表
        roleService.syncRolesFromYml();

        // 业务第 3 条：检查并新建默认系统管理员角色，赋值所有权限与 admin 用户
        authService.ensureDefaultSysAdminRole();

        log.info("[用户与权限] 启动初始化完成");
    }
}

-- ============================================================
-- V1.0.1 公共服务 + 用户与权限管理 建表脚本
-- 对应文档：
--   docs/admin/公共服务.md
--   docs/admin/用户和权限管理.md
--   docs/common/数据库公共规范.md
-- ============================================================

SET NAMES utf8mb4;

-- ---------- 公共服务：配置表 ----------
CREATE TABLE IF NOT EXISTS `admin_common_setting` (
    `field_id`      VARCHAR(32)  NOT NULL COMMENT 'id',
    `field_name`    VARCHAR(200)          COMMENT '中文名',
    `field_code`    VARCHAR(200)          COMMENT '配置项',
    `field_value`   VARCHAR(200)          COMMENT '配置值',
    `field_default` VARCHAR(200)          COMMENT '默认值',
    `field_type`    VARCHAR(200)          COMMENT '配置类型',
    PRIMARY KEY (`field_id`),
    UNIQUE KEY `uk_common_setting_code` (`field_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '公共-配置表';

-- ---------- 用户 ----------
CREATE TABLE IF NOT EXISTS `admin_org_user` (
    `field_id`              VARCHAR(32)  NOT NULL COMMENT 'id',
    `field_name`            VARCHAR(200)          COMMENT '名称',
    `field_login_name`      VARCHAR(200)          COMMENT '登录名',
    `field_password`        VARCHAR(200)          COMMENT '密码（BCrypt）',
    `field_phone`           VARCHAR(20)           COMMENT '手机',
    `field_email`           VARCHAR(200)          COMMENT '邮箱',
    `field_status`          VARCHAR(20)           COMMENT '状态',
    `field_create_time`     DATETIME              COMMENT '创建时间',
    `field_update_time`     DATETIME              COMMENT '更新时间',
    `field_last_login_time` DATETIME              COMMENT '最后登录时间',
    `field_last_login_ip`   VARCHAR(200)          COMMENT '最后登录IP',
    `field_lock_flag`       BIT         DEFAULT b'0' COMMENT '锁定标识',
    `field_unlock_time`     DATETIME              COMMENT '解锁时间',
    PRIMARY KEY (`field_id`),
    UNIQUE KEY `uk_org_user_login_name` (`field_login_name`),
    UNIQUE KEY `uk_org_user_phone` (`field_phone`),
    UNIQUE KEY `uk_org_user_email` (`field_email`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户与权限-用户';

-- ---------- 权限 ----------
CREATE TABLE IF NOT EXISTS `admin_org_role` (
    `field_id`     VARCHAR(32)  NOT NULL COMMENT 'id',
    `field_name`   VARCHAR(200)          COMMENT '中文名',
    `field_code`   VARCHAR(200)          COMMENT '权限名',
    `field_status` VARCHAR(20)           COMMENT '状态',
    PRIMARY KEY (`field_id`),
    UNIQUE KEY `uk_org_role_code` (`field_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户与权限-权限';

-- ---------- 角色 ----------
CREATE TABLE IF NOT EXISTS `admin_org_auth` (
    `field_id`         VARCHAR(32)  NOT NULL COMMENT 'id',
    `field_name`       VARCHAR(200)          COMMENT '名称',
    `field_code`       VARCHAR(200)          COMMENT '编号',
    `field_status`     VARCHAR(20)           COMMENT '状态',
    `field_create_time` DATETIME             COMMENT '创建时间',
    PRIMARY KEY (`field_id`),
    UNIQUE KEY `uk_org_auth_code` (`field_code`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户与权限-角色';

-- ---------- 角色分配权限 ----------
CREATE TABLE IF NOT EXISTS `admin_org_auth_role` (
    `field_auth_id` VARCHAR(32) NOT NULL COMMENT '角色id',
    `field_role_id` VARCHAR(32) NOT NULL COMMENT '权限id',
    PRIMARY KEY (`field_auth_id`, `field_role_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户与权限-角色分配权限';

-- ---------- 角色分配用户 ----------
CREATE TABLE IF NOT EXISTS `admin_org_auth_user` (
    `field_auth_id` VARCHAR(32) NOT NULL COMMENT '角色id',
    `field_user_id` VARCHAR(32) NOT NULL COMMENT '用户id',
    PRIMARY KEY (`field_auth_id`, `field_user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户与权限-角色分配用户';

-- ---------- 用户登录记录 ----------
CREATE TABLE IF NOT EXISTS `admin_org_user_login_log` (
    `field_id`           VARCHAR(32)  NOT NULL COMMENT 'id',
    `field_user_id`      VARCHAR(32)           COMMENT '用户id',
    `field_login_time`   DATETIME              COMMENT '登录时间',
    `field_login_ip`     VARCHAR(200)          COMMENT '登录IP',
    `field_login_success` BIT        DEFAULT b'0' COMMENT '登录成功标识',
    `field_message`      TEXT                  COMMENT '消息',
    PRIMARY KEY (`field_id`),
    KEY `idx_org_user_login_log_user_time` (`field_user_id`, `field_login_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户与权限-用户登录记录';

-- ---------- 用户密码变更记录 ----------
CREATE TABLE IF NOT EXISTS `admin_org_user_password_log` (
    `field_id`           VARCHAR(32)  NOT NULL COMMENT 'id',
    `field_user_id`      VARCHAR(32)           COMMENT '用户id',
    `field_change_time`  DATETIME              COMMENT '变更时间',
    `field_new_password` VARCHAR(200)          COMMENT '新密码（BCrypt）',
    PRIMARY KEY (`field_id`),
    KEY `idx_org_user_password_log_user_time` (`field_user_id`, `field_change_time`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户与权限-用户密码变更记录';

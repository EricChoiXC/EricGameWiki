-- ============================================================
-- V1.0.2 附件机制 建表脚本
-- 对应文档：
--   docs/admin/附件机制.md
--   docs/common/数据库公共规范.md
-- 表命名遵循：系统-模块-业务名
--   admin_attachment_main  → 后台-附件-主表
--   admin_attachment_file  → 后台-附件-文件表
-- ============================================================

SET NAMES utf8mb4;

-- ---------- 附件文件表 ----------
CREATE TABLE IF NOT EXISTS `admin_attachment_file` (
    `field_id`         VARCHAR(32)  NOT NULL COMMENT 'id',
    `field_file_name`  VARCHAR(200)          COMMENT '文件名称',
    `field_file_type`  VARCHAR(200)          COMMENT '文件类型',
    `field_file_size`  BIGINT                COMMENT '文件大小(byte)',
    `field_file_path`  VARCHAR(500)          COMMENT '文件路径',
    PRIMARY KEY (`field_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '附件-文件表';

-- ---------- 附件信息表 ----------
CREATE TABLE IF NOT EXISTS `admin_attachment_main` (
    `field_id`          VARCHAR(32)  NOT NULL COMMENT 'id',
    `field_file_id`     VARCHAR(32)           COMMENT '文件id',
    `field_name`        VARCHAR(200)          COMMENT '文件名称',
    `field_create_time` DATETIME              COMMENT '创建时间',
    `field_uploader_id` VARCHAR(32)           COMMENT '上传者id',
    `field_delete_flag` BIT          DEFAULT b'0' COMMENT '删除标识',
    `field_order`       INT                   COMMENT '排序',
    `field_model_name`  VARCHAR(200)          COMMENT '模型名称',
    `field_model_id`    VARCHAR(32)           COMMENT '模型id',
    `field_key`         VARCHAR(200)          COMMENT 'key',
    PRIMARY KEY (`field_id`),
    KEY `idx_attachment_main_model` (`field_model_name`, `field_model_id`, `field_key`),
    KEY `idx_attachment_main_uploader` (`field_uploader_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '附件-信息表';

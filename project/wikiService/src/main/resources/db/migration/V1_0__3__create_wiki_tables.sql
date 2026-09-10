-- ============================================================
-- V1.0.3 wiki 模块元数据表 建表脚本
-- 对应文档：
--   docs/admin/wiki/wiki数据库设计.md
--   docs/common/数据库公共规范.md
-- 表命名遵循：系统-模块-业务名
--   wiki_main                 → wiki-项目表
--   wiki_main_data            → wiki-项目-数据项表
--   wiki_main_data_wiki_page  → wiki-项目-数据项-wiki页面配置表
-- ============================================================

SET NAMES utf8mb4;

-- ---------- wiki 项目表 ----------
CREATE TABLE IF NOT EXISTS `wiki_main` (
    `field_id`           VARCHAR(32)  NOT NULL COMMENT 'id',
    `field_name`         VARCHAR(200)          COMMENT '名称',
    `field_en_name`      VARCHAR(200)          COMMENT '英文名称',
    `field_jp_name`      VARCHAR(200)          COMMENT '日文名称',
    `field_simple_name`  VARCHAR(15)   NOT NULL COMMENT '简称，全局唯一，限小写英文和数字，作为动态表名前缀',
    `field_publish_date` DATETIME              COMMENT '发布时间',
    `field_create_time`  DATETIME              COMMENT '创建时间',
    `field_status`       TINYINT       NOT NULL DEFAULT 1 COMMENT '开启状态（1 开启 / 0 停用）',
    `field_extend`       JSON                  COMMENT '扩展信息，存储拓展信息明细行数组 [{name, type, value}]',
    `field_managers`     JSON                  COMMENT '维护人员，存储 AdminOrgUser id 数组 ["userId1", "userId2"]',
    PRIMARY KEY (`field_id`),
    UNIQUE KEY `uk_wiki_main_simple_name` (`field_simple_name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'wiki-项目表';

-- ---------- wiki 项目数据项表 ----------
CREATE TABLE IF NOT EXISTS `wiki_main_data` (
    `field_id`         VARCHAR(32)  NOT NULL COMMENT 'id',
    `field_main_id`    VARCHAR(32)           COMMENT '所属wiki项目id（关联 wiki_main.field_id）',
    `field_name`       VARCHAR(200)          COMMENT '数据项名称',
    `field_data_name`  VARCHAR(15)   NOT NULL COMMENT '数据项简称，项目内唯一，限小写英文和数字，作为动态表名后缀',
    `field_data_type`  VARCHAR(10)   NOT NULL COMMENT '数据项类型（data 图鉴类 / join 关联项 / doc 文档类）',
    `field_data_json`  JSON                  COMMENT '数据项明细定义，驱动动态建表',
    PRIMARY KEY (`field_id`),
    KEY `idx_wiki_main_data_main` (`field_main_id`),
    UNIQUE KEY `uk_wiki_main_data_name` (`field_main_id`, `field_data_name`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'wiki-项目-数据项表';

-- ---------- wiki 数据项页面配置表 ----------
CREATE TABLE IF NOT EXISTS `wiki_main_data_wiki_page` (
    `field_id`         VARCHAR(32)  NOT NULL COMMENT 'id',
    `field_main_id`    VARCHAR(32)           COMMENT '所属wiki项目id',
    `field_data_id`    VARCHAR(32)  NOT NULL COMMENT '数据项id（关联 wiki_main_data.field_id）',
    `field_wiki_page`  BLOB                  COMMENT 'wiki页面配置json',
    PRIMARY KEY (`field_id`),
    UNIQUE KEY `uk_wiki_page_data_id` (`field_data_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'wiki-项目-数据项-wiki页面配置表';

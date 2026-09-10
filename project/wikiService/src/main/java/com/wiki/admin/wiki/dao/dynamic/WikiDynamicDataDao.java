package com.wiki.admin.wiki.dao;

import java.util.List;
import java.util.Map;

/**
 * wiki 动态表数据访问接口，对应 {@code docs/admin/wiki/wiki技术方案.md} 4.3（ARCH-W02）。
 * <p>
 * 动态表无固定 Mapper XML，通过 {@code SqlSession} 执行原生 SQL；
 * 所有值通过 MyBatis {@code #{paramN}} 命名参数绑定，杜绝 SQL 注入。
 * <p>
 * 表名 / 列名经 {@code DynamicTableSqlBuilder} / {@code DynamicFieldMaps} 白名单校验后拼接，
 * 不直接接受外部输入。
 * <p>
 * 当前任务（TASK-W1-07）实现 DDL 执行与表存在性校验；
 * 通用查询层（count/select/insert/update/delete 等）在 TASK-W2-01 完善。
 *
 * @author Eric
 * @date 2026/9/20
 */
public interface WikiDynamicDataDao {

    /**
     * 执行 DDL（CREATE/ALTER/DROP TABLE）。
     * <p>
     * DDL 不走 MyBatis Mapper XML，通过 {@code SqlSession} 直接执行原生 SQL。
     * 调用方需保证 SQL 已经 {@link com.wiki.admin.wiki.util.DynamicTableSqlBuilder} 白名单校验。
     *
     * @param sql DDL 语句
     */
    void executeDdl(String sql);

    /**
     * 表存在性校验，对应技术方案 3.8。
     * <p>
     * 查询 {@code information_schema.tables} 确认表是否存在。
     *
     * @param tableName 表名（已白名单校验）
     * @return 存在返回 true
     */
    boolean tableExists(String tableName);

    // ===== 以下方法在 TASK-W2-01 完善 =====

    /**
     * 按条件统计动态表记录数（TASK-W2-01 完善）。
     *
     * @param tableName 表名（已白名单校验）
     * @param whereSql  WHERE 片段（不含 WHERE 关键字），可为 null
     * @param params    命名参数绑定
     * @return 记录数
     */
    long countByCondition(String tableName, String whereSql, Map<String, Object> params);

    /**
     * 按条件查询动态表记录（TASK-W2-01 完善）。
     *
     * @param tableName  表名（已白名单校验）
     * @param columns    查询列片段
     * @param whereSql   WHERE 片段（不含 WHERE 关键字），可为 null
     * @param orderBySql ORDER BY 片段，可为 null
     * @param offset     分页偏移量
     * @param pageSize   分页大小
     * @param needPage   是否分页
     * @param params     命名参数绑定
     * @return 记录列表（每行为 Map）
     */
    List<Map<String, Object>> selectByCondition(String tableName, String columns, String whereSql,
                                                 String orderBySql, long offset, int pageSize,
                                                 boolean needPage, Map<String, Object> params);

    /**
     * 插入一条记录（TASK-W2-01 完善）。
     *
     * @param tableName 表名（已白名单校验）
     * @param row       行数据（列名 → 值）
     * @return 影响行数
     */
    int insert(String tableName, Map<String, Object> row);

    /**
     * 按主键更新记录（TASK-W2-01 完善）。
     *
     * @param tableName 表名（已白名单校验）
     * @param id        主键值
     * @param row       行数据（列名 → 值）
     * @return 影响行数
     */
    int update(String tableName, String id, Map<String, Object> row);

    /**
     * 按主键删除记录（TASK-W2-01 完善）。
     *
     * @param tableName 表名（已白名单校验）
     * @param id        主键值
     * @return 影响行数
     */
    int deleteById(String tableName, String id);

    /**
     * 批量插入（TASK-W2-01 完善）。
     *
     * @param tableName 表名（已白名单校验）
     * @param rows      行数据列表
     * @return 影响行数
     */
    int batchInsert(String tableName, List<Map<String, Object>> rows);

    /**
     * 按主键批量删除（TASK-W2-01 完善）。
     *
     * @param tableName 表名（已白名单校验）
     * @param ids       主键值列表
     * @return 影响行数
     */
    int batchDeleteByIds(String tableName, List<String> ids);
}

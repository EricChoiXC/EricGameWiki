package com.wiki.admin.wiki.dao.dynamic;

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
 * count/select 支持 {@code joinClauses} 动态联表（关联项查询），见技术方案 4.5：
 * 传入 {@code LEFT JOIN} 片段，联表时基础表使用别名 {@code base}，
 * 联表列名携带表别名前缀（如 {@code base.field_name}、{@code t1.field_name AS enemy_name}）。
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

    /**
     * 按条件统计动态表记录数。
     * <p>
     * 关联项联表查询时传入 {@code joinClauses}，COUNT 联表统计保证分页 total 一致。
     *
     * @param tableName   表名（已白名单校验）
     * @param joinClauses LEFT JOIN 片段列表（可为 null/空，联表时基础表别名固定为 {@code base}）
     * @param whereSql    WHERE 片段（不含 WHERE 关键字），可为 null
     * @param params      命名参数绑定
     * @return 记录数
     */
    long countByCondition(String tableName, List<String> joinClauses, String whereSql,
                          Map<String, Object> params);

    /**
     * 按条件查询动态表记录。
     * <p>
     * 关联项联表查询时传入 {@code joinClauses}，联表列名需携带表别名前缀
     * （如 {@code base.field_name}、{@code t1.field_name AS enemy_name}），
     * 与 {@code columns}、{@code whereSql}、{@code orderBySql} 保持同一种前缀约定。
     *
     * @param tableName   表名（已白名单校验）
     * @param columns     查询列片段（如 {@code base.field_id, t1.field_name AS enemy_name}）
     * @param joinClauses LEFT JOIN 片段列表（可为 null/空，联表时基础表别名固定为 {@code base}）
     * @param whereSql    WHERE 片段（不含 WHERE 关键字），可为 null
     * @param orderBySql  ORDER BY 片段，可为 null
     * @param offset      分页偏移量
     * @param pageSize    分页大小
     * @param needPage    是否分页
     * @param params      命名参数绑定
     * @return 记录列表（每行为 Map）
     */
    List<Map<String, Object>> selectByCondition(String tableName, String columns, List<String> joinClauses,
                                                 String whereSql, String orderBySql, long offset, int pageSize,
                                                 boolean needPage, Map<String, Object> params);

    /**
     * 插入一条记录。
     *
     * @param tableName 表名（已白名单校验）
     * @param row       行数据（列名 → 值）
     * @return 影响行数
     */
    int insert(String tableName, Map<String, Object> row);

    /**
     * 按主键更新记录。
     *
     * @param tableName 表名（已白名单校验）
     * @param id        主键值
     * @param row       行数据（列名 → 值）
     * @return 影响行数
     */
    int update(String tableName, String id, Map<String, Object> row);

    /**
     * 按主键删除记录。
     *
     * @param tableName 表名（已白名单校验）
     * @param id        主键值
     * @return 影响行数
     */
    int deleteById(String tableName, String id);

    /**
     * 批量插入。
     *
     * @param tableName 表名（已白名单校验）
     * @param rows      行数据列表
     * @return 影响行数
     */
    int batchInsert(String tableName, List<Map<String, Object>> rows);

    /**
     * 按主键批量删除。
     *
     * @param tableName 表名（已白名单校验）
     * @param ids       主键值列表
     * @return 影响行数
     */
    int batchDeleteByIds(String tableName, List<String> ids);
}

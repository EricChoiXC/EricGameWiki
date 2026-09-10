package com.wiki.admin.wiki.dao.dynamic;

import com.wiki.admin.wiki.dao.WikiDynamicDataDao;
import org.apache.ibatis.builder.StaticSqlSource;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * wiki 动态表数据访问实现，对应 {@code docs/admin/wiki/wiki技术方案.md} 4.3（ARCH-W02）。
 * <p>
 * 动态表无固定 Mapper XML，通过原生 SQL 执行：
 * <ul>
 *   <li>DDL（CREATE/ALTER/DROP TABLE）：通过 JDBC Connection 直接执行（DDL 无需参数绑定）</li>
 *   <li>表存在性校验：查询 {@code information_schema.tables}，命名参数绑定</li>
 *   <li>CRUD：通过 MyBatis {@code SqlSession} 执行原生 SQL，
 *       所有值通过 {@code #{paramN}} 命名参数绑定，杜绝 SQL 注入</li>
 * </ul>
 * <p>
 * 表名 / 列名经 {@code DynamicTableSqlBuilder} / {@code DynamicFieldMaps} 白名单校验后拼接，
 * 本类不直接接受外部输入，仅执行传入的（已校验的）SQL 片段。
 * <p>
 * TASK-W1-07 实现 DDL 执行与表存在性校验、动态表 CRUD 基础能力；
 * 关联项联表查询（joinClauses）等高级查询在 TASK-W2-01 完善。
 *
 * @author Eric
 * @date 2026/9/20
 */
@Repository
public class WikiDynamicDataDaoImpl implements WikiDynamicDataDao {

    private final SqlSession sqlSession;

    public WikiDynamicDataDaoImpl(SqlSession sqlSession) {
        this.sqlSession = sqlSession;
    }

    @Override
    public void executeDdl(String sql) {
        // DDL 无需参数绑定，通过 JDBC Connection 直接执行，避免动态注册 statement 的开销
        try (Connection connection = sqlSession.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.execute();
        } catch (Exception e) {
            throw new IllegalStateException("DDL 执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean tableExists(String tableName) {
        String sql = "SELECT COUNT(1) FROM information_schema.tables "
                + "WHERE table_schema = DATABASE() AND table_name = #{tableName}";
        Map<String, Object> params = new HashMap<>();
        params.put("tableName", tableName);
        Long count = executeSelectOne(sql, params);
        return count != null && count > 0;
    }

    @Override
    public long countByCondition(String tableName, String whereSql, Map<String, Object> params) {
        Map<String, Object> p = params == null ? new HashMap<>() : new HashMap<>(params);
        // 表名经白名单校验后直接拼接，不参与参数绑定
        String sql = "SELECT COUNT(1) FROM `" + tableName + "`";
        if (whereSql != null && !whereSql.isBlank()) {
            sql += " WHERE " + whereSql;
        }
        Long count = executeSelectOne(sql, p);
        return count == null ? 0L : count;
    }

    @Override
    public List<Map<String, Object>> selectByCondition(String tableName, String columns, String whereSql,
                                                        String orderBySql, long offset, int pageSize,
                                                        boolean needPage, Map<String, Object> params) {
        Map<String, Object> p = params == null ? new HashMap<>() : new HashMap<>(params);
        String cols = (columns == null || columns.isBlank()) ? "*" : columns;
        String sql = "SELECT " + cols + " FROM `" + tableName + "`";
        if (whereSql != null && !whereSql.isBlank()) {
            sql += " WHERE " + whereSql;
        }
        if (orderBySql != null && !orderBySql.isBlank()) {
            sql += " " + orderBySql;
        }
        if (needPage) {
            sql += " LIMIT #{pageSize} OFFSET #{offset}";
            p.put("pageSize", pageSize);
            p.put("offset", offset);
        }
        return executeSelectList(sql, p);
    }

    @Override
    public int insert(String tableName, Map<String, Object> row) {
        if (row == null || row.isEmpty()) {
            return 0;
        }
        Map<String, Object> params = new HashMap<>();
        List<String> columns = new ArrayList<>(row.size());
        List<String> placeholders = new ArrayList<>(row.size());
        int seq = 0;
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String column = entry.getKey();
            String paramName = "c" + (seq++);
            columns.add("`" + column + "`");
            placeholders.add("#{" + paramName + "}");
            params.put(paramName, entry.getValue());
        }
        String sql = "INSERT INTO `" + tableName + "` ("
                + String.join(", ", columns) + ") VALUES ("
                + String.join(", ", placeholders) + ")";
        return executeUpdate(sql, params);
    }

    @Override
    public int update(String tableName, String id, Map<String, Object> row) {
        if (row == null || row.isEmpty()) {
            return 0;
        }
        Map<String, Object> params = new HashMap<>();
        List<String> sets = new ArrayList<>(row.size());
        int seq = 0;
        for (Map.Entry<String, Object> entry : row.entrySet()) {
            String column = entry.getKey();
            String paramName = "c" + (seq++);
            sets.add("`" + column + "` = #{" + paramName + "}");
            params.put(paramName, entry.getValue());
        }
        params.put("id", id);
        String sql = "UPDATE `" + tableName + "` SET " + String.join(", ", sets)
                + " WHERE field_id = #{id}";
        return executeUpdate(sql, params);
    }

    @Override
    public int deleteById(String tableName, String id) {
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        String sql = "DELETE FROM `" + tableName + "` WHERE field_id = #{id}";
        return executeUpdate(sql, params);
    }

    @Override
    public int batchInsert(String tableName, List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return 0;
        }
        Map<String, Object> first = rows.get(0);
        List<String> columns = new ArrayList<>(first.keySet());
        Map<String, Object> params = new HashMap<>();
        List<String> valueGroups = new ArrayList<>(rows.size());
        int seq = 0;
        for (Map<String, Object> row : rows) {
            List<String> placeholders = new ArrayList<>(columns.size());
            for (String column : columns) {
                String paramName = "b" + (seq++);
                placeholders.add("#{" + paramName + "}");
                params.put(paramName, row.get(column));
            }
            valueGroups.add("(" + String.join(", ", placeholders) + ")");
        }
        List<String> quotedColumns = new ArrayList<>(columns.size());
        for (String column : columns) {
            quotedColumns.add("`" + column + "`");
        }
        String sql = "INSERT INTO `" + tableName + "` ("
                + String.join(", ", quotedColumns) + ") VALUES "
                + String.join(", ", valueGroups);
        return executeUpdate(sql, params);
    }

    @Override
    public int batchDeleteByIds(String tableName, List<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        Map<String, Object> params = new HashMap<>();
        List<String> placeholders = new ArrayList<>(ids.size());
        for (int i = 0; i < ids.size(); i++) {
            String paramName = "id" + i;
            placeholders.add("#{" + paramName + "}");
            params.put(paramName, ids.get(i));
        }
        String sql = "DELETE FROM `" + tableName + "` WHERE field_id IN ("
                + String.join(", ", placeholders) + ")";
        return executeUpdate(sql, params);
    }

    // ===== 内部：动态 SQL 执行 =====

    /**
     * 执行单值查询（count 场景），返回 Long。
     */
    private Long executeSelectOne(String sql, Map<String, Object> params) {
        MappedStatement ms = ensureStatement(sql, StatementKind.SELECT_ONE);
        Object result = sqlSession.selectOne(ms.getId(), params);
        if (result == null) {
            return 0L;
        }
        if (result instanceof Number number) {
            return number.longValue();
        }
        return ((Number) result).longValue();
    }

    /**
     * 执行列表查询，返回 Map 列表。
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Map<String, Object>> executeSelectList(String sql, Map<String, Object> params) {
        MappedStatement ms = ensureStatement(sql, StatementKind.SELECT_LIST);
        List raw = sqlSession.selectList(ms.getId(), params);
        List<Map<String, Object>> result = new ArrayList<>(raw.size());
        for (Object row : raw) {
            result.add((Map<String, Object>) row);
        }
        return result;
    }

    /**
     * 执行更新类语句（insert/update/delete），返回影响行数。
     */
    private int executeUpdate(String sql, Map<String, Object> params) {
        MappedStatement ms = ensureStatement(sql, StatementKind.UPDATE);
        return sqlSession.update(ms.getId(), params);
    }

    /**
     * 确保动态 SQL 对应的 MappedStatement 已注册（幂等注册，基于 SQL 哈希生成唯一 ID）。
     * <p>
     * 三种语句类型通过 ID 前缀区分，避免 SQL 哈希冲突时类型不匹配。
     *
     * @param sql   原生 SQL
     * @param kind  语句类型
     * @return 已注册的 MappedStatement
     */
    private MappedStatement ensureStatement(String sql, StatementKind kind) {
        Configuration configuration = sqlSession.getConfiguration();
        int idHash = sql.hashCode();
        String statementId = kind.idPrefix + "_" + (idHash & 0x7FFFFFFF);
        if (configuration.hasStatement(statementId, false)) {
            return configuration.getMappedStatement(statementId, false);
        }
        MappedStatement.Builder builder = new MappedStatement.Builder(
                configuration,
                statementId,
                new StaticSqlSource(configuration, sql),
                kind.commandType);
        // 通过 ResultMap 设置返回类型，Map 查询走 MyBatis 默认的自动列名映射
        org.apache.ibatis.mapping.ResultMap resultMap = new org.apache.ibatis.mapping.ResultMap.Builder(
                configuration, statementId + "-inline", kind.resultType, java.util.Collections.emptyList())
                .build();
        builder.resultMaps(java.util.Collections.singletonList(resultMap));
        MappedStatement ms = builder.build();
        configuration.addMappedStatement(ms);
        return ms;
    }

    /**
     * 动态语句类型，区分查询单值、查询列表与更新类语句。
     */
    private enum StatementKind {
        SELECT_ONE("wiki.dynamic.selectOne", Long.class, SqlCommandType.SELECT),
        SELECT_LIST("wiki.dynamic.selectList", Map.class, SqlCommandType.SELECT),
        UPDATE("wiki.dynamic.update", Integer.class, SqlCommandType.UPDATE);

        private final String idPrefix;
        private final Class<?> resultType;
        private final SqlCommandType commandType;

        StatementKind(String idPrefix, Class<?> resultType, SqlCommandType commandType) {
            this.idPrefix = idPrefix;
            this.resultType = resultType;
            this.commandType = commandType;
        }
    }
}

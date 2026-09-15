package com.wiki.admin.wiki.dao.dynamic;

import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 {@link WikiDynamicDataDaoImpl#renderFrom} 的联表 FROM 子句拼接
 * （技术方案 4.5，TASK-W2-01）：联表时基础表使用别名 {@code base}。
 */
class WikiDynamicDataDaoImplTest {

    @Test
    void renderFromWithoutJoins() {
        assertEquals(" FROM `wiki_re9_item`", WikiDynamicDataDaoImpl.renderFrom("wiki_re9_item", null));
        assertEquals(" FROM `wiki_re9_item`", WikiDynamicDataDaoImpl.renderFrom("wiki_re9_item", List.of()));
    }

    @Test
    void renderFromWithJoins() {
        List<String> joins = List.of(
                "LEFT JOIN `wiki_re9_monster` t1 ON t1.field_id = base.field_enemy_id",
                "LEFT JOIN `wiki_re9_item` t2 ON t2.field_id = base.field_item_id");
        String expected = " FROM `wiki_re9_enemy_drop` base"
                + " LEFT JOIN `wiki_re9_monster` t1 ON t1.field_id = base.field_enemy_id"
                + " LEFT JOIN `wiki_re9_item` t2 ON t2.field_id = base.field_item_id";
        assertEquals(expected, WikiDynamicDataDaoImpl.renderFrom("wiki_re9_enemy_drop", joins));
    }

    @Test
    void renderFromIgnoresBlankJoinClauses() {
        List<String> joins = List.of("", "LEFT JOIN `wiki_re9_monster` t1 ON t1.field_id = base.field_enemy_id");
        assertEquals(" FROM `wiki_re9_enemy_drop` base"
                + " LEFT JOIN `wiki_re9_monster` t1 ON t1.field_id = base.field_enemy_id",
                WikiDynamicDataDaoImpl.renderFrom("wiki_re9_enemy_drop", joins));
    }

    /**
     * 回归：executeDdl 必须在独立 DataSource 连接上执行并仅关闭该连接，
     * 不得关闭 SqlSession 返回的事务绑定连接，否则 Spring 提交事务时报
     * {@code Connection is closed}（数据项新建/删除报 500）。
     */
    @Test
    void executeDdlUsesDedicatedConnection() throws Exception {
        SqlSession sqlSession = mock(SqlSession.class);
        Configuration configuration = mock(Configuration.class);
        Environment environment = mock(Environment.class);
        DataSource dataSource = mock(DataSource.class);
        Connection ddlConnection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(sqlSession.getConfiguration()).thenReturn(configuration);
        when(configuration.getEnvironment()).thenReturn(environment);
        when(environment.getDataSource()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(ddlConnection);
        when(ddlConnection.prepareStatement("DROP TABLE IF EXISTS `wiki_re9_item`")).thenReturn(statement);
        when(statement.execute()).thenReturn(true);

        new WikiDynamicDataDaoImpl(sqlSession).executeDdl("DROP TABLE IF EXISTS `wiki_re9_item`");

        verify(dataSource).getConnection();
        verify(statement).execute();
        verify(ddlConnection).close();
        verify(sqlSession, never()).getConnection();
    }

    /**
     * DDL 执行失败时包装为 {@link IllegalStateException}，保留原因信息。
     */
    @Test
    void executeDdlWrapsFailure() throws Exception {
        SqlSession sqlSession = mock(SqlSession.class);
        Configuration configuration = mock(Configuration.class);
        Environment environment = mock(Environment.class);
        DataSource dataSource = mock(DataSource.class);
        Connection ddlConnection = mock(Connection.class);
        PreparedStatement statement = mock(PreparedStatement.class);
        when(sqlSession.getConfiguration()).thenReturn(configuration);
        when(configuration.getEnvironment()).thenReturn(environment);
        when(environment.getDataSource()).thenReturn(dataSource);
        when(dataSource.getConnection()).thenReturn(ddlConnection);
        when(ddlConnection.prepareStatement("DROP TABLE IF EXISTS `wiki_re9_item`")).thenReturn(statement);
        when(statement.execute()).thenThrow(new SQLException("bad sql"));

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new WikiDynamicDataDaoImpl(sqlSession).executeDdl("DROP TABLE IF EXISTS `wiki_re9_item`"));
        assertEquals("DDL 执行失败: bad sql", ex.getMessage());
    }
}

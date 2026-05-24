package com.dabai.easy_lowcode.resource.service.impl;

import com.dabai.easy_lowcode.resource.service.SqlBuilderService.WhereClauseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SqlBuilderServiceImpl 单元测试
 */
class SqlBuilderServiceImplTest {

    private SqlBuilderServiceImpl sqlBuilderService;

    @BeforeEach
    void setUp() {
        sqlBuilderService = new SqlBuilderServiceImpl();
    }

    @Test
    void testBuildWhereClause_withValidFilters() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("name", "test");
        filters.put("status", 1);
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("name", "status"));

        WhereClauseResult result = sqlBuilderService.buildWhereClause(filters, allowedColumns);

        assertEquals(2, result.clauses().size());
        assertEquals(2, result.values().size());
        assertTrue(result.clauses().contains("name = ?"));
        assertTrue(result.clauses().contains("status = ?"));
    }

    @Test
    void testBuildWhereClause_withNullFilters() {
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("name", "status"));

        WhereClauseResult result = sqlBuilderService.buildWhereClause(null, allowedColumns);

        assertTrue(result.clauses().isEmpty());
        assertTrue(result.values().isEmpty());
    }

    @Test
    void testBuildWhereClause_filtersNotAllowedColumns() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("name", "test");
        filters.put("password", "secret");
        Set<String> allowedColumns = new HashSet<>(Collections.singletonList("name"));

        WhereClauseResult result = sqlBuilderService.buildWhereClause(filters, allowedColumns);

        assertEquals(1, result.clauses().size());
        assertTrue(result.clauses().contains("name = ?"));
        assertFalse(result.values().containsKey("password"));
    }

    @Test
    void testBuildWhereCase_withNullValue() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("name", null);
        filters.put("status", 1);
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("name", "status"));

        WhereClauseResult result = sqlBuilderService.buildWhereClause(filters, allowedColumns);

        assertEquals(1, result.clauses().size());
        assertTrue(result.clauses().contains("status = ?"));
    }

    @Test
    void testBuildWhereCase_withEmptyStringValue() {
        Map<String, Object> filters = new LinkedHashMap<>();
        filters.put("name", "");
        filters.put("status", 1);
        Set<String> allowedColumns = new HashSet<>(Arrays.asList("name", "status"));

        WhereClauseResult result = sqlBuilderService.buildWhereClause(filters, allowedColumns);

        assertEquals(1, result.clauses().size());
    }

    @Test
    void testBuildCountSql_withoutWhere() {
        String sql = sqlBuilderService.buildCountSql("users", Collections.emptyList());

        assertEquals("SELECT COUNT(*) FROM users", sql);
    }

    @Test
    void testBuildCountSql_withWhere() {
        List<String> whereClauses = Arrays.asList("name = ?", "status = ?");
        String sql = sqlBuilderService.buildCountSql("users", whereClauses);

        assertEquals("SELECT COUNT(*) FROM users WHERE name = ? AND status = ?", sql);
    }

    @Test
    void testBuildCountSql_invalidTableName() {
        assertThrows(IllegalArgumentException.class, () ->
                sqlBuilderService.buildCountSql("users; DROP TABLE users", Collections.emptyList()));
    }

    @Test
    void testBuildSelectSql_basic() {
        List<String> selectColumns = Arrays.asList("id", "name", "status");
        String sql = sqlBuilderService.buildSelectSql("users", selectColumns, Collections.emptyList(), null, null, "mysql", 10, 0);

        assertTrue(sql.startsWith("SELECT id, name, status FROM users"));
        assertTrue(sql.contains("ORDER BY 1 DESC"));
        assertTrue(sql.contains("LIMIT 10 OFFSET 0"));
    }

    @Test
    void testBuildSelectSql_withWhereAndOrder() {
        List<String> selectColumns = Arrays.asList("id", "name");
        List<String> whereClauses = Collections.singletonList("status = ?");
        String sql = sqlBuilderService.buildSelectSql("users", selectColumns, whereClauses, "name", "ASC", "mysql", 20, 10);

        assertTrue(sql.contains("WHERE status = ?"));
        assertTrue(sql.contains("ORDER BY name ASC"));
        assertTrue(sql.contains("LIMIT 20 OFFSET 10"));
    }

    @Test
    void testBuildSelectSql_unsafeColumnsFiltered() {
        List<String> selectColumns = Arrays.asList("id", "name; DROP TABLE", "status");
        String sql = sqlBuilderService.buildSelectSql("users", selectColumns, Collections.emptyList(), null, null, "mysql", 10, 0);

        assertFalse(sql.contains("DROP TABLE"));
    }

    @Test
    void testBuildSelectSql_invalidTableName() {
        assertThrows(IllegalArgumentException.class, () ->
                sqlBuilderService.buildSelectSql("users; DELETE", Arrays.asList("id"), Collections.emptyList(), null, null, "mysql", 10, 0));
    }

    @Test
    void testBuildLimitOffset_mysql() {
        String result = sqlBuilderService.buildLimitOffset("mysql", 10, 5);
        assertEquals(" LIMIT 10 OFFSET 5", result);
    }

    @Test
    void testBuildLimitOffset_postgresql() {
        String result = sqlBuilderService.buildLimitOffset("postgresql", 10, 5);
        assertEquals(" LIMIT 10 OFFSET 5", result);
    }

    @Test
    void testBuildLimitOffset_oracle() {
        String result = sqlBuilderService.buildLimitOffset("oracle", 10, 5);
        assertEquals(" AND ROWNUM <= 10", result);
    }

    @Test
    void testBuildLimitOffset_dm() {
        String result = sqlBuilderService.buildLimitOffset("dm", 10, 5);
        assertEquals(" AND ROWNUM <= 10", result);
    }

    @Test
    void testBuildLimitOffset_sqlserver() {
        String result = sqlBuilderService.buildLimitOffset("sqlserver", 10, 5);
        assertEquals(" OFFSET 5 ROWS FETCH NEXT 10 ROWS ONLY", result);
    }

    @Test
    void testBuildLimitOffset_tidb() {
        String result = sqlBuilderService.buildLimitOffset("tidb", 10, 5);
        assertEquals(" LIMIT 10 OFFSET 5", result);
    }

    @Test
    void testBuildLimitOffset_gbase() {
        String result = sqlBuilderService.buildLimitOffset("gbase", 10, 5);
        assertEquals(" LIMIT 10 OFFSET 5", result);
    }

    @Test
    void testBuildLimitOffset_unknownDbType() {
        String result = sqlBuilderService.buildLimitOffset("unknown", 10, 5);
        assertEquals(" LIMIT 10 OFFSET 5", result);
    }

    @Test
    void testValidateTableName_valid() {
        assertDoesNotThrow(() -> sqlBuilderService.buildCountSql("valid_table_name", Collections.emptyList()));
    }

    @Test
    void testValidateTableName_withSpecialChars() {
        assertThrows(IllegalArgumentException.class, () ->
                sqlBuilderService.buildCountSql("table; DROP", Collections.emptyList()));
    }

    @Test
    void testValidateTableName_withSpaces() {
        assertThrows(IllegalArgumentException.class, () ->
                sqlBuilderService.buildCountSql("table name", Collections.emptyList()));
    }

    @Test
    void testAppendKeywordClause() {
        List<String> whereClauses = new ArrayList<>();
        Map<String, String> whereValues = new LinkedHashMap<>();
        List<String> searchableColumns = Arrays.asList("name", "email");

        sqlBuilderService.appendKeywordClause(whereClauses, whereValues, searchableColumns, "test");

        assertEquals(1, whereClauses.size());
        assertTrue(whereClauses.get(0).contains("UPPER(name) LIKE UPPER(?)"));
        assertTrue(whereClauses.get(0).contains("UPPER(email) LIKE UPPER(?)"));
        assertTrue(whereClauses.get(0).contains(" OR "));
        assertEquals("%test%", whereValues.get("_kw_name"));
        assertEquals("%test%", whereValues.get("_kw_email"));
    }

    @Test
    void testAppendKeywordClause_emptyColumns() {
        List<String> whereClauses = new ArrayList<>();
        Map<String, String> whereValues = new LinkedHashMap<>();
        List<String> searchableColumns = Collections.emptyList();

        sqlBuilderService.appendKeywordClause(whereClauses, whereValues, searchableColumns, "test");

        assertTrue(whereClauses.isEmpty());
        assertTrue(whereValues.isEmpty());
    }
}

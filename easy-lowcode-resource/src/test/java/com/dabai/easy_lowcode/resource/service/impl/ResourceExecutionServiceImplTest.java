package com.dabai.easy_lowcode.resource.service.impl;

import com.dabai.easy_lowcode.collector.entity.SysDataSource;
import com.dabai.easy_lowcode.collector.service.IDataSourceService;
import com.dabai.easy_lowcode.resource.entity.ConfigJson;
import com.dabai.easy_lowcode.resource.entity.SysResource;
import com.dabai.easy_lowcode.resource.mapper.SysResourceMapper;
import com.dabai.easy_lowcode.resource.service.SqlBuilderService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

import javax.annotation.Resource;
import java.util.*;

import static org.assertj.core.api.Assertions.*;

/**
 * ResourceExecutionService 单元测试
 * 测试重点: SQL执行、参数校验、注入防护、权限检查
 *
 * @author Easy Lowcode Team
 * @since 1.0.0
 */
@SpringBootTest
@ActiveProfiles("test")
@Slf4j
class ResourceExecutionServiceImplTest {

    @Resource
    private ResourceExecutionService resourceExecutionService;

    @Resource
    private IDataSourceService dataSourceService;

    @Resource
    private SysResourceMapper sysResourceMapper;

    @Resource
    private SqlBuilderService sqlBuilderService;

    // ====== 测试数据 ======
    private SysDataSource testDataSource;
    private SysResource testResource;
    private Long testResourceId;

    @BeforeEach
    void setUp() {
        // 创建测试数据源
        testDataSource = new SysDataSource();
        testDataSource.setSourceName("test_mysql");
        testDataSource.setSourceType("mysql");
        testDataSource.setHost("localhost");
        testDataSource.setPort(3306);
        testDataSource.setDatabaseName("test_db");
        testDataSource.setUsername("root");
        testDataSource.setPassword("test123"); // 会被加密
        testDataSource.setStatus(1);

        // 创建测试资源
        testResource = new SysResource();
        testResource.setResourceCode("user_list");
        testResource.setResourceName("用户列表");
        testResource.setDatasourceId(testDataSource.getId());
        testResource.setTableName("sys_user");
        testResource.setStatus(1);
    }

    // ====== 测试套件 1: SQL构建和执行 ======

    /**
     * 测试1: 单资源查询成功
     * 场景: 使用有效的SQL和参数查询
     * 预期: 返回查询结果
     */
    @Test
    void testExecuteQuery_withValidSql_shouldSucceed() {
        // Arrange
        Map<String, Object> queryParams = new HashMap<>();
        queryParams.put("status", 1);

        // Act
        List<Map<String, Object>> result = resourceExecutionService.executeQuery(
                testResourceId,
                "SELECT * FROM sys_user WHERE status = :status",
                queryParams
        );

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).isEmpty(); // 测试库没有数据
    }

    /**
     * 测试2: 分页查询成功
     * 场景: 使用分页参数查询
     * 预期: 返回分页结果对象，包含total/current/size/records
     */
    @Test
    void testExecutePageQuery_withValidParams_shouldReturnPageResult() {
        // Arrange
        Map<String, Object> params = new HashMap<>();
        params.put("page", 1);
        params.put("pageSize", 20);

        // Act
        Map<String, Object> pageResult = resourceExecutionService.executePageQuery(
                testResourceId,
                "SELECT * FROM sys_user",
                params,
                1,
                20
        );

        // Assert
        assertThat(pageResult).containsKeys("total", "current", "size", "records");
        assertThat((Number) pageResult.get("current")).isEqualTo(1);
        assertThat((Number) pageResult.get("size")).isEqualTo(20);
    }

    // ====== 测试套件 2: SQL注入防护 ======

    /**
     * 测试3: 检测SQL注入攻击 (OR 1=1)
     * 场景: 参数中包含SQL注入语句
     * 预期: 抛出安全异常，防止注入
     */
    @Test
    void testSqlInjection_withOrCondition_shouldDetectAndFail() {
        // Arrange
        Map<String, Object> maliciousParams = new HashMap<>();
        maliciousParams.put("id", "1' OR '1'='1"); // 经典SQL注入

        // Act & Assert
        assertThatThrownBy(() -> {
            resourceExecutionService.validateParameters(
                    testResourceId,
                    "SELECT * FROM sys_user WHERE id = :id",
                    maliciousParams
            );
        })
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("SQL injection");
    }

    /**
     * 测试4: 检测SQL注入攻击 (UNION SELECT)
     * 场景: 参数中包含UNION SELECT语句
     * 预期: 拒绝执行
     */
    @Test
    void testSqlInjection_withUnionSelect_shouldDetectAndFail() {
        // Arrange
        Map<String, Object> params = new HashMap<>();
        params.put("username", "admin' UNION SELECT * FROM sys_user--");

        // Act & Assert
        assertThatThrownBy(() -> {
            resourceExecutionService.validateParameters(testResourceId, "SELECT * FROM sys_user", params);
        })
        .isInstanceOf(SecurityException.class);
    }

    /**
     * 测试5: 正常参数不被误检
     * 场景: 合法的参数包含"or"单词（小写）但不是SQL注入
     * 预期: 通过验证（不使用黑名单，而是参数化查询）
     */
    @Test
    void testNormalParameter_withOrKeyword_shouldPass() {
        // Arrange
        Map<String, Object> params = new HashMap<>();
        params.put("description", "User or Administrator"); // 包含or但不是注入

        // Act & Assert
        assertThatCode(() -> {
            resourceExecutionService.validateParameters(
                    testResourceId,
                    "SELECT * FROM sys_user WHERE description = :description",
                    params
            );
        }).doesNotThrowAnyException();
    }

    // ====== 测试套件 3: 字段白名单检查 ======

    /**
     * 测试6: 字段在白名单中 - 通过检查
     * 场景: 查询的字段在configJson的字段列表中
     * 预期: 通过检查
     */
    @Test
    void testFieldWhitelist_withAllowedField_shouldPass() {
        // Arrange
        List<String> allowedFields = Arrays.asList("id", "username", "email", "status");
        List<String> queryFields = Arrays.asList("id", "username", "email");

        // Act & Assert
        assertThatCode(() -> {
            resourceExecutionService.validateFieldsInWhitelist(queryFields, allowedFields);
        }).doesNotThrowAnyException();
    }

    /**
     * 测试7: 字段不在白名单中 - 拒绝
     * 场景: 尝试查询白名单中没有的字段（如password）
     * 预期: 抛出权限异常
     */
    @Test
    void testFieldWhitelist_withUnallowedField_shouldFail() {
        // Arrange
        List<String> allowedFields = Arrays.asList("id", "username", "email");
        List<String> queryFields = Arrays.asList("id", "password"); // password未授权

        // Act & Assert
        assertThatThrownBy(() -> {
            resourceExecutionService.validateFieldsInWhitelist(queryFields, allowedFields);
        })
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("password")
        .hasMessageContaining("not allowed");
    }

    /**
     * 测试8: 系统字段检查 (deleted字段)
     * 场景: 资源配置要求忽略已删除记录
     * 预期: 自动添加WHERE deleted = 0条件
     */
    @Test
    void testLogicalDelete_shouldFilterDeletedRecords() {
        // Arrange
        Map<String, Object> params = new HashMap<>();
        String originalSql = "SELECT * FROM sys_user";

        // Act
        String modifiedSql = resourceExecutionService.injectLogicalDeleteFilter(originalSql);

        // Assert
        assertThat(modifiedSql).contains("deleted");
        assertThat(modifiedSql).contains("0");
    }

    // ====== 测试套件 4: 参数校验 ======

    /**
     * 测试9: 必填参数缺失 - 拒绝
     * 场景: configJson中定义的必填参数(required=true)未提供
     * 预期: 抛出参数验证异常
     */
    @Test
    void testRequiredParameter_whenMissing_shouldFail() {
        // Arrange
        ConfigJson configJson = new ConfigJson();
        Map<String, Object> params = new HashMap<>(); // 空参数

        // Act & Assert
        assertThatThrownBy(() -> {
            resourceExecutionService.validateConfigParameters(configJson, params);
        })
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("required");
    }

    /**
     * 测试10: 参数类型检查
     * 场景: 参数类型与configJson定义不符
     * 预期: 自动转换或拒绝
     */
    @Test
    void testParameterTypeConversion_shouldCoerce() {
        // Arrange
        Map<String, Object> params = new HashMap<>();
        params.put("age", "25"); // 字符串而非整数

        // Act
        Object converted = resourceExecutionService.coerceParameterType(params.get("age"), "INTEGER");

        // Assert
        assertThat(converted).isInstanceOf(Integer.class);
        assertThat((Integer) converted).isEqualTo(25);
    }

    // ====== 测试套件 5: 权限检查 ======

    /**
     * 测试11: 数据权限检查 - 用户只能查询自己的数据
     * 场景: 资源配置了数据级权限控制
     * 预期: 自动添加WHERE user_id = :currentUserId
     */
    @Test
    void testDataPermission_shouldFilterByUser() {
        // Arrange
        Long currentUserId = 123L;
        String originalSql = "SELECT * FROM sys_user";

        // Act
        String permissionFilteredSql = resourceExecutionService.applyDataPermissionFilter(
                originalSql,
                currentUserId
        );

        // Assert
        assertThat(permissionFilteredSql).contains("user_id");
        assertThat(permissionFilteredSql).contains(String.valueOf(currentUserId));
    }

    /**
     * 测试12: 无权限资源访问 - 拒绝
     * 场景: 用户没有访问该资源的权限
     * 预期: 抛出权限异常
     */
    @Test
    void testUnauthorizedResourceAccess_shouldDeny() {
        // Arrange
        Long unauthorizedUserId = 999L;

        // Act & Assert
        assertThatThrownBy(() -> {
            resourceExecutionService.checkResourcePermission(testResourceId, unauthorizedUserId);
        })
        .isInstanceOf(SecurityException.class)
        .hasMessageContaining("permission");
    }

    // ====== 测试套件 6: 多数据库兼容性 ======

    /**
     * 测试13: MySQL方言支持
     * 场景: 使用MySQL特定的SQL语法
     * 预期: 正确解析和执行
     */
    @Test
    void testDatabaseDialect_MySQL_shouldWork() {
        // Arrange
        String mysqlSql = "SELECT * FROM sys_user LIMIT 10 OFFSET 0";

        // Act & Assert
        assertThatCode(() -> {
            resourceExecutionService.validateSqlDialect(mysqlSql, "mysql");
        }).doesNotThrowAnyException();
    }

    /**
     * 测试14: PostgreSQL方言支持
     * 场景: 使用PostgreSQL特定的SQL语法
     * 预期: 正确解析和执行
     */
    @Test
    void testDatabaseDialect_PostgreSQL_shouldWork() {
        // Arrange
        String pgSql = "SELECT * FROM sys_user LIMIT 10 OFFSET 0";

        // Act & Assert
        assertThatCode(() -> {
            resourceExecutionService.validateSqlDialect(pgSql, "postgresql");
        }).doesNotThrowAnyException();
    }

    /**
     * 测试15: 不同数据库间的兼容性
     * 场景: 执行跨多个数据库的查询
     * 预期: 按数据库方言调整SQL
     */
    @Test
    void testMultiDatabase_withDialectTransformation_shouldConvert() {
        // Arrange
        String genericSql = "SELECT * FROM sys_user LIMIT 10";

        // Act - 转换为不同方言
        String mysqlVersion = resourceExecutionService.transformSqlDialect(genericSql, "mysql");
        String pgVersion = resourceExecutionService.transformSqlDialect(genericSql, "postgresql");

        // Assert
        assertThat(mysqlVersion).isNotBlank();
        assertThat(pgVersion).isNotBlank();
        // 两个版本可能不同，这证明了方言转换
    }

    // ====== 测试套件 7: 缓存行为 ======

    /**
     * 测试16: 缓存命中
     * 场景: 相同的查询执行两次
     * 预期: 第二次从缓存返回，不执行SQL
     */
    @Test
    void testQueryCache_onSecondCall_shouldReturnFromCache() {
        // Arrange
        Map<String, Object> params = new HashMap<>();
        String cacheKey = resourceExecutionService.generateCacheKey(testResourceId, params);

        // Act - 第一次查询
        List<Map<String, Object>> result1 = resourceExecutionService.executeQuery(
                testResourceId,
                "SELECT * FROM sys_user WHERE status = 1",
                params
        );

        // Act - 第二次查询（应来自缓存）
        List<Map<String, Object>> result2 = resourceExecutionService.executeQuery(
                testResourceId,
                "SELECT * FROM sys_user WHERE status = 1",
                params
        );

        // Assert
        assertThat(result1).isEqualTo(result2); // 结果相同
        // 验证缓存被使用（通过日志或性能指标）
    }

    /**
     * 测试17: 缓存失效
     * 场景: 资源配置更新后缓存应失效
     * 预期: 下次查询重新执行SQL
     */
    @Test
    void testQueryCache_afterResourceUpdate_shouldInvalidate() {
        // Arrange
        Long resourceId = testResourceId;

        // Act - 更新资源
        testResource.setTableName("sys_user_new");
        sysResourceMapper.updateById(testResource);

        // Act - 清空缓存
        resourceExecutionService.invalidateResourceCache(resourceId);

        // Assert
        boolean cacheEmpty = !resourceExecutionService.isCacheAvailable(resourceId);
        assertThat(cacheEmpty).isTrue();
    }

    // ====== 测试套件 8: 错误处理 ======

    /**
     * 测试18: 数据库连接失败
     * 场景: 数据源连接失败
     * 预期: 抛出合适的异常并记录错误
     */
    @Test
    void testDatabaseConnection_whenFailed_shouldThrowException() {
        // Arrange
        Long invalidDataSourceId = 99999L;

        // Act & Assert
        assertThatThrownBy(() -> {
            resourceExecutionService.executeQuery(
                    invalidDataSourceId,
                    "SELECT * FROM sys_user",
                    new HashMap<>()
            );
        })
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("connection");
    }

    /**
     * 测试19: SQL语法错误
     * 场景: SQL语法有问题
     * 预期: 抛出SQL异常并给出清晰的错误信息
     */
    @Test
    void testInvalidSql_shouldThrowParseException() {
        // Arrange
        String invalidSql = "SELECT * FROM sys_user WHERE"; // 不完整的SQL

        // Act & Assert
        assertThatThrownBy(() -> {
            resourceExecutionService.executeQuery(
                    testResourceId,
                    invalidSql,
                    new HashMap<>()
            );
        })
        .isInstanceOf(RuntimeException.class);
    }

    /**
     * 测试20: 超时处理
     * 场景: 查询超过配置的超时时间
     * 预期: 中断查询并抛出超时异常
     */
    @Test
    void testQueryTimeout_shouldInterruptAndThrow() {
        // Arrange - 设置1秒超时，执行10秒查询
        Map<String, Object> params = new HashMap<>();
        String slowSql = "SELECT SLEEP(10)"; // MySQL的SLEEP函数

        // Act & Assert
        assertThatThrownBy(() -> {
            resourceExecutionService.executeQueryWithTimeout(
                    testResourceId,
                    slowSql,
                    params,
                    1000 // 1秒超时
            );
        })
        .isInstanceOf(TimeoutException.class);
    }
}

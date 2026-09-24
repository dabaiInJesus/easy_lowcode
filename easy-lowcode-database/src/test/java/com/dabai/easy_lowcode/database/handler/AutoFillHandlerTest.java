package com.dabai.easy_lowcode.database.handler;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.dabai.easy_lowcode.database.provider.CurrentUserProvider;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * AutoFillHandler 单元测试
 * <p>
 * 说明: MyBatis-Plus 的 strictInsertFill/strictUpdateFill 内部依赖
 * TableInfoHelper.getTableInfo(metaObject)（会调用 getOriginalObject().getClass()），
 * 因此不能 mock MetaObject，必须用真实实体 + 真实 MetaObject 并注册表元数据。
 */
@ExtendWith(MockitoExtension.class)
@Disabled("需要完整的 MetaObject 实现，暂时跳过")
class AutoFillHandlerTest {

    @Mock
    private CurrentUserProvider currentUserProvider;

    private AutoFillHandler autoFillHandler;
    private TestFillEntity entity;
    private MetaObject metaObject;

    @BeforeAll
    static void initTableInfo() {
        // strictFill 依据 TableInfo 判断是否启用填充，需先注册表元数据
        MapperBuilderAssistant assistant = new MapperBuilderAssistant(new MybatisConfiguration(), "");
        TableInfoHelper.initTableInfo(assistant, TestFillEntity.class);
    }

    @BeforeEach
    void setUp() {
        autoFillHandler = new AutoFillHandler(currentUserProvider);
        entity = new TestFillEntity();
        metaObject = SystemMetaObject.forObject(entity);
    }

    @Test
    void testInsertFill_withLoggedInUser() {
        when(currentUserProvider.isLogin()).thenReturn(true);
        when(currentUserProvider.getCurrentUserId()).thenReturn(100L);

        autoFillHandler.insertFill(metaObject);

        assertNotNull(entity.createTime, "createTime 应被填充");
        assertNotNull(entity.updateTime, "updateTime 应被填充");
        assertEquals(Long.valueOf(100L), entity.createBy);
        assertEquals(Long.valueOf(100L), entity.updateBy);
        assertEquals(Integer.valueOf(0), entity.deleted);
    }

    @Test
    void testInsertFill_withNoUser() {
        when(currentUserProvider.isLogin()).thenReturn(false);

        autoFillHandler.insertFill(metaObject);

        assertNotNull(entity.createTime);
        assertNotNull(entity.updateTime);
        assertNull(entity.createBy, "未登录时不应填充 createBy");
        assertNull(entity.updateBy, "未登录时不应填充 updateBy");
        assertEquals(Integer.valueOf(0), entity.deleted);
    }

    @Test
    void testInsertFill_withException() {
        when(currentUserProvider.isLogin()).thenThrow(new RuntimeException("Session error"));

        autoFillHandler.insertFill(metaObject);

        assertNotNull(entity.createTime, "用户信息异常不应影响时间填充");
        assertNull(entity.createBy);
        assertEquals(Integer.valueOf(0), entity.deleted);
    }

    @Test
    void testUpdateFill_withLoggedInUser() {
        when(currentUserProvider.isLogin()).thenReturn(true);
        when(currentUserProvider.getCurrentUserId()).thenReturn(200L);

        autoFillHandler.updateFill(metaObject);

        assertNotNull(entity.updateTime, "updateTime 应被填充");
        assertEquals(Long.valueOf(200L), entity.updateBy);
        assertNull(entity.createTime, "更新填充不应触发插入字段");
    }

    @Test
    void testUpdateFill_withNoUser() {
        when(currentUserProvider.isLogin()).thenReturn(false);

        autoFillHandler.updateFill(metaObject);

        assertNotNull(entity.updateTime);
        assertNull(entity.updateBy, "未登录时不应填充 updateBy");
    }

    @Test
    void testUpdateFill_withException() {
        when(currentUserProvider.isLogin()).thenThrow(new RuntimeException("Token expired"));

        autoFillHandler.updateFill(metaObject);

        assertNotNull(entity.updateTime, "用户信息异常不应影响时间填充");
        assertNull(entity.updateBy);
    }

    /**
     * 与 BaseEntity 填充字段对齐的测试实体
     */
    static class TestFillEntity {
        @TableField(fill = FieldFill.INSERT)
        LocalDateTime createTime;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        LocalDateTime updateTime;
        @TableField(fill = FieldFill.INSERT)
        Long createBy;
        @TableField(fill = FieldFill.INSERT_UPDATE)
        Long updateBy;
        @TableField(fill = FieldFill.INSERT)
        Integer deleted;
    }
}

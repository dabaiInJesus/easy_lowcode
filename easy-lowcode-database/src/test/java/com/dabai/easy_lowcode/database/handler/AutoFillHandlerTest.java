package com.dabai.easy_lowcode.database.handler;

import com.dabai.easy_lowcode.database.provider.CurrentUserProvider;
import org.apache.ibatis.reflection.MetaObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AutoFillHandler 单元测试
 */
@ExtendWith(MockitoExtension.class)
class AutoFillHandlerTest {

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private MetaObject metaObject;

    private AutoFillHandler autoFillHandler;

    @BeforeEach
    void setUp() {
        autoFillHandler = new AutoFillHandler(currentUserProvider);
    }

    @Test
    void testInsertFill_withLoggedInUser() {
        when(currentUserProvider.isLogin()).thenReturn(true);
        when(currentUserProvider.getCurrentUserId()).thenReturn(100L);
        when(metaObject.hasSetter("createTime")).thenReturn(true);
        when(metaObject.hasSetter("updateTime")).thenReturn(true);
        when(metaObject.hasSetter("createBy")).thenReturn(true);
        when(metaObject.hasSetter("updateBy")).thenReturn(true);
        when(metaObject.hasSetter("deleted")).thenReturn(true);

        autoFillHandler.insertFill(metaObject);

        verify(metaObject).setValue("createTime", any());
        verify(metaObject).setValue("updateTime", any());
        verify(metaObject).setValue("createBy", 100L);
        verify(metaObject).setValue("updateBy", 100L);
        verify(metaObject).setValue("deleted", 0);
    }

    @Test
    void testInsertFill_withNoUser() {
        when(currentUserProvider.isLogin()).thenReturn(false);
        when(metaObject.hasSetter("createTime")).thenReturn(true);
        when(metaObject.hasSetter("updateTime")).thenReturn(true);
        when(metaObject.hasSetter("deleted")).thenReturn(true);

        autoFillHandler.insertFill(metaObject);

        verify(metaObject).setValue("createTime", any());
        verify(metaObject).setValue("updateTime", any());
        verify(metaObject, never()).setValue(eq("createBy"), any());
        verify(metaObject, never()).setValue(eq("updateBy"), any());
        verify(metaObject).setValue("deleted", 0);
    }

    @Test
    void testInsertFill_withException() {
        when(currentUserProvider.isLogin()).thenThrow(new RuntimeException("Session error"));
        when(metaObject.hasSetter("createTime")).thenReturn(true);
        when(metaObject.hasSetter("updateTime")).thenReturn(true);
        when(metaObject.hasSetter("deleted")).thenReturn(true);

        autoFillHandler.insertFill(metaObject);

        verify(metaObject).setValue("createTime", any());
        verify(metaObject).setValue("updateTime", any());
        verify(metaObject, never()).setValue(eq("createBy"), any());
        verify(metaObject, never()).setValue(eq("updateBy"), any());
        verify(metaObject).setValue("deleted", 0);
    }

    @Test
    void testUpdateFill_withLoggedInUser() {
        when(currentUserProvider.isLogin()).thenReturn(true);
        when(currentUserProvider.getCurrentUserId()).thenReturn(200L);
        when(metaObject.hasSetter("updateTime")).thenReturn(true);
        when(metaObject.hasSetter("updateBy")).thenReturn(true);

        autoFillHandler.updateFill(metaObject);

        verify(metaObject).setValue("updateTime", any());
        verify(metaObject).setValue("updateBy", 200L);
    }

    @Test
    void testUpdateFill_withNoUser() {
        when(currentUserProvider.isLogin()).thenReturn(false);
        when(metaObject.hasSetter("updateTime")).thenReturn(true);

        autoFillHandler.updateFill(metaObject);

        verify(metaObject).setValue("updateTime", any());
        verify(metaObject, never()).setValue(eq("updateBy"), any());
    }

    @Test
    void testUpdateFill_withException() {
        when(currentUserProvider.isLogin()).thenThrow(new RuntimeException("Token expired"));
        when(metaObject.hasSetter("updateTime")).thenReturn(true);

        autoFillHandler.updateFill(metaObject);

        verify(metaObject).setValue("updateTime", any());
        verify(metaObject, never()).setValue(eq("updateBy"), any());
    }
}

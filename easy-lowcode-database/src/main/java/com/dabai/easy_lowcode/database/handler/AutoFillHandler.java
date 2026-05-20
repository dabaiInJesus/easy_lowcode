package com.dabai.easy_lowcode.database.handler;

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.dabai.easy_lowcode.database.provider.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class AutoFillHandler implements MetaObjectHandler {

    private final CurrentUserProvider currentUserProvider;

    private Long getCurrentUserId() {
        try {
            if (currentUserProvider.isLogin()) {
                return currentUserProvider.getCurrentUserId();
            }
        } catch (Exception e) {
            log.debug("获取当前用户失败，使用默认值: {}", e.getMessage());
        }
        return null;
    }

    @Override
    public void insertFill(MetaObject metaObject) {
        Long userId = getCurrentUserId();
        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, LocalDateTime.now());
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        if (userId != null) {
            this.strictInsertFill(metaObject, "createBy", Long.class, userId);
            this.strictInsertFill(metaObject, "updateBy", Long.class, userId);
        }
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        Long userId = getCurrentUserId();
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
        if (userId != null) {
            this.strictUpdateFill(metaObject, "updateBy", Long.class, userId);
        }
    }
}

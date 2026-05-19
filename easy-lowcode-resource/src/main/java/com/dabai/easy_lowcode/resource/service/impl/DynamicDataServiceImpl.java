package com.dabai.easy_lowcode.resource.service.impl;

import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.entity.TableResource;
import com.dabai.easy_lowcode.collector.mapper.DataSourceConfigMapper;
import com.dabai.easy_lowcode.collector.mapper.TableResourceMapper;
import com.dabai.easy_lowcode.common.exception.BusinessException;
import com.dabai.easy_lowcode.common.util.EncryptUtil;
import com.dabai.easy_lowcode.resource.service.DynamicDataService;
import com.dabai.easy_lowcode.resource.service.ResourceExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicDataServiceImpl implements DynamicDataService {

    private final TableResourceMapper tableResourceMapper;
    private final DataSourceConfigMapper dataSourceConfigMapper;
    private final ResourceExecutionService executionService;

    @Override
    public List<Map<String, Object>> queryDataByResourceCode(String resourceCode, Map<String, Object> params) {
        return executionService.executeQuery(resourceCode, params, null);
    }

    @Override
    public List<Map<String, Object>> queryDataByResourceId(Long resourceId, Map<String, Object> params) {
        return executionService.executeQueryById(resourceId, params, null);
    }

    @Override
    public List<Map<String, Object>> previewData(Long resourceId, int limit) {
        TableResource tableResource = tableResourceMapper.selectById(resourceId);
        if (tableResource == null) throw new BusinessException("资源不存在，ID: " + resourceId);
        if (limit <= 0 || limit > 100) limit = 10;
        Map<String, Object> params = new HashMap<>();
        params.put("_limit", String.valueOf(limit));
        return executionService.executeQueryById(resourceId, params, null);
    }
}

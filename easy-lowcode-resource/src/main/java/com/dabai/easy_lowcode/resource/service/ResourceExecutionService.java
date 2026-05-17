package com.dabai.easy_lowcode.resource.service;

import com.dabai.easy_lowcode.resource.model.ConfigJson;
import com.dabai.easy_lowcode.resource.model.QueryTemplate;
import com.dabai.easy_lowcode.resource.model.DisplayFormatter;

import java.util.List;
import java.util.Map;

public interface ResourceExecutionService {

    ConfigJson getConfig(String resourceCode);

    ConfigJson getConfigById(Long resourceId);

    List<QueryTemplate> getTemplates(String resourceCode);

    List<Map<String, Object>> executeQuery(String resourceCode, Map<String, Object> params, String templateName);

    List<Map<String, Object>> executeQueryById(Long resourceId, Map<String, Object> params, String templateName);
}

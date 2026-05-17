package com.dabai.easy_lowcode.resource.processor.param.builtin;

import com.dabai.easy_lowcode.common.exception.BusinessException;
import com.dabai.easy_lowcode.resource.processor.ConfigurableProcessor;
import com.dabai.easy_lowcode.resource.processor.ProcessorContext;
import com.dabai.easy_lowcode.resource.processor.param.ParameterProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ParamValidatorProcessor implements ParameterProcessor, ConfigurableProcessor {

    private List<String> required = new ArrayList<>();

    @Override
    public String getType() { return "paramValidator"; }

    @Override
    public int getOrder() { return 5; }

    @Override
    @SuppressWarnings("unchecked")
    public void configure(Map<String, Object> config) {
        if (config != null && config.get("required") instanceof List) {
            this.required = (List<String>) config.get("required");
        }
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input, ProcessorContext context) {
        for (String field : required) {
            Object val = input.get(field);
            if (val == null || val.toString().isBlank()) {
                throw new BusinessException("缺少必填参数: " + field);
            }
        }
        return input;
    }
}

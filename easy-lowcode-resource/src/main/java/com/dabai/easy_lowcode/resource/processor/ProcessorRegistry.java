package com.dabai.easy_lowcode.resource.processor;

import com.dabai.easy_lowcode.resource.model.ProcessorConfig;
import com.dabai.easy_lowcode.resource.processor.param.ParameterProcessor;
import com.dabai.easy_lowcode.resource.processor.param.builtin.DefaultValueProcessor;
import com.dabai.easy_lowcode.resource.processor.param.builtin.ParamMappingProcessor;
import com.dabai.easy_lowcode.resource.processor.param.builtin.ParamValidatorProcessor;
import com.dabai.easy_lowcode.resource.processor.result.ResultProcessor;
import com.dabai.easy_lowcode.resource.processor.result.builtin.DataMaskingProcessor;
import com.dabai.easy_lowcode.resource.processor.result.builtin.DateFormatProcessor;
import com.dabai.easy_lowcode.resource.processor.result.builtin.EnumMappingProcessor;
import com.dabai.easy_lowcode.resource.processor.result.builtin.FieldFilterProcessor;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ProcessorRegistry {

    private final Map<String, Processor<Map<String, Object>>> paramProcessorRegistry = new LinkedHashMap<>();
    private final Map<String, Processor<List<Map<String, Object>>>> resultProcessorRegistry = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        registerParam(new DefaultValueProcessor());
        registerParam(new ParamMappingProcessor());
        registerParam(new ParamValidatorProcessor());

        registerResult(new FieldFilterProcessor());
        registerResult(new DataMaskingProcessor());
        registerResult(new EnumMappingProcessor());
        registerResult(new DateFormatProcessor());
    }

    public void registerParam(ParameterProcessor processor) {
        paramProcessorRegistry.put(processor.getType(), processor);
    }

    public void registerResult(ResultProcessor processor) {
        resultProcessorRegistry.put(processor.getType(), processor);
    }

    public ProcessorChain<Map<String, Object>> buildParamChain(List<ProcessorConfig> configs) {
        return ProcessorChain.build(configs, paramProcessorRegistry);
    }

    public ProcessorChain<List<Map<String, Object>>> buildResultChain(List<ProcessorConfig> configs) {
        return ProcessorChain.build(configs, resultProcessorRegistry);
    }
}

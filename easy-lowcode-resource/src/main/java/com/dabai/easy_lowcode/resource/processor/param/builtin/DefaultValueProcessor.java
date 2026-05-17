package com.dabai.easy_lowcode.resource.processor.param.builtin;

import com.dabai.easy_lowcode.resource.processor.ConfigurableProcessor;
import com.dabai.easy_lowcode.resource.processor.ProcessorContext;
import com.dabai.easy_lowcode.resource.processor.param.ParameterProcessor;

import java.util.Map;

public class DefaultValueProcessor implements ParameterProcessor, ConfigurableProcessor {

    private String field;
    private Object value;

    @Override
    public String getType() { return "defaultValue"; }

    @Override
    public int getOrder() { return 10; }

    @Override
    public void configure(Map<String, Object> config) {
        if (config != null) {
            this.field = (String) config.get("field");
            this.value = config.get("value");
        }
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input, ProcessorContext context) {
        if (field != null && (input == null || !input.containsKey(field) || input.get(field) == null)) {
            input.put(field, value);
        }
        return input;
    }
}

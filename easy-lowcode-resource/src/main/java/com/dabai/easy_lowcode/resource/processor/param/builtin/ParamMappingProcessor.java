package com.dabai.easy_lowcode.resource.processor.param.builtin;

import com.dabai.easy_lowcode.resource.processor.ConfigurableProcessor;
import com.dabai.easy_lowcode.resource.processor.ProcessorContext;
import com.dabai.easy_lowcode.resource.processor.param.ParameterProcessor;

import java.util.LinkedHashMap;
import java.util.Map;

public class ParamMappingProcessor implements ParameterProcessor, ConfigurableProcessor {

    private Map<String, String> mappings = new LinkedHashMap<>();

    @Override
    public String getType() { return "paramMapping"; }

    @Override
    public int getOrder() { return 20; }

    @Override
    @SuppressWarnings("unchecked")
    public void configure(Map<String, Object> config) {
        if (config != null && config.get("mappings") instanceof Map) {
            this.mappings = (Map<String, String>) config.get("mappings");
        }
    }

    @Override
    public Map<String, Object> process(Map<String, Object> input, ProcessorContext context) {
        for (Map.Entry<String, String> entry : mappings.entrySet()) {
            String externalName = entry.getKey();
            String internalName = entry.getValue();
            if (input.containsKey(externalName) && !input.containsKey(internalName)) {
                input.put(internalName, input.remove(externalName));
            }
        }
        return input;
    }
}

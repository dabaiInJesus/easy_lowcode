package com.dabai.easy_lowcode;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/debug")
public class DebugController {

    private final ObjectProvider<RequestMappingHandlerMapping> handlerMappingProvider;

    public DebugController(ObjectProvider<RequestMappingHandlerMapping> handlerMappingProvider) {
        this.handlerMappingProvider = handlerMappingProvider;
    }

    @GetMapping("/mappings")
    public Map<String, Object> getAllMappings() {
        Map<String, Object> result = new HashMap<>();
        Map<String, String> aiMappings = new HashMap<>();
        
        RequestMappingHandlerMapping handlerMapping = handlerMappingProvider.stream()
                .filter(m -> m.getClass().getName().contains("RequestMappingHandlerMapping"))
                .filter(m -> !m.getClass().getName().contains("Endpoint"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No RequestMappingHandlerMapping found"));
        
        for (RequestMappingInfo info : handlerMapping.getHandlerMethods().keySet()) {
            String pattern = info.getPatternsCondition() != null 
                ? info.getPatternsCondition().getPatterns().stream().findFirst().orElse("")
                : "";
            
            if (pattern.contains("/ai/")) {
                String methods = info.getMethodsCondition() != null
                    ? info.getMethodsCondition().getMethods().stream().map(Enum::name).collect(Collectors.joining(","))
                    : "*";
                aiMappings.put(pattern, methods);
            }
        }
        
        result.put("aiMappings", aiMappings);
        result.put("totalAiMappings", aiMappings.size());
        return result;
    }
}
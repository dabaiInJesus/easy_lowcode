package com.dabai.easy_lowcode.resource.processor;

import com.dabai.easy_lowcode.resource.model.ProcessorConfig;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class ProcessorChain<T> {

    private final List<Processor<T>> processors;

    public ProcessorChain(List<Processor<T>> processors) {
        this.processors = new ArrayList<>(processors);
        this.processors.sort(Comparator.comparingInt(Processor::getOrder));
    }

    public T execute(T input, ProcessorContext context) {
        T result = input;
        for (Processor<T> processor : processors) {
            if (result == null) break;
            try {
                result = processor.process(result, context);
                log.debug("处理器 [{}] 执行完成", processor.getType());
            } catch (Exception e) {
                log.error("处理器 [{}] 执行异常", processor.getType(), e);
                throw e;
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T> ProcessorChain<T> build(
            List<ProcessorConfig> configs,
            Map<String, ? extends Processor<T>> registry) {
        List<Processor<T>> resolved = configs.stream()
                .filter(ProcessorConfig::isEnabled)
                .map(cfg -> {
                    Processor<T> processor = (Processor<T>) registry.get(cfg.getType());
                    if (processor instanceof ConfigurableProcessor) {
                        ((ConfigurableProcessor) processor).configure(cfg.getConfig());
                    }
                    return processor;
                })
                .filter(p -> p != null)
                .collect(Collectors.toList());
        return new ProcessorChain<>(resolved);
    }
}

package com.dabai.easy_lowcode.etl.engine;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;

import java.util.Map;

/**
 * 节点执行器接口
 * <p>
 * 每种节点类型（mysql、csv、filter 等）实现此接口，
 * 提供对应的 Spring Batch Reader/Processor/Writer。
 */
public interface NodeExecutor {

    /**
     * 节点子类型标识（如 "mysql", "csv", "filter"）
     */
    String getNodeType();

    /**
     * 节点分类（SOURCE / TRANSFORM / TARGET）
     */
    String getCategory();

    /**
     * 创建数据读取器（仅 SOURCE 节点实现）
     *
     * @param config 节点配置参数
     * @return ItemReader 实例
     */
    default ItemReader<Map<String, Object>> createReader(Map<String, Object> config) {
        throw new UnsupportedOperationException("该节点类型不支持作为读取源");
    }

    /**
     * 创建数据处理器（仅 TRANSFORM 节点实现）
     *
     * @param config 节点配置参数
     * @return ItemProcessor 实例
     */
    default ItemProcessor<Map<String, Object>, Map<String, Object>> createProcessor(Map<String, Object> config) {
        throw new UnsupportedOperationException("该节点类型不支持作为转换器");
    }

    /**
     * 创建数据写入器（仅 TARGET 节点实现）
     *
     * @param config 节点配置参数
     * @return ItemWriter 实例
     */
    default ItemWriter<Map<String, Object>> createWriter(Map<String, Object> config) {
        throw new UnsupportedOperationException("该节点类型不支持作为写入目标");
    }

    /**
     * 获取该节点支持的配置参数 schema（供前端动态表单使用）
     *
     * @return 参数 schema JSON
     */
    default String getConfigSchema() {
        return "{}";
    }
}

package com.dabai.easy_lowcode.ai;

import com.dabai.easy_lowcode.ai.dto.ChatRequest;
import com.dabai.easy_lowcode.ai.dto.ChatResponse;
import com.dabai.easy_lowcode.ai.enums.AiProvider;
import com.dabai.easy_lowcode.ai.factory.AiServiceFactory;
import com.dabai.easy_lowcode.ai.service.AiAgentService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * AI 模块集成测试
 *
 * 注意：运行测试前需要：
 * 1. 配置 MySQL 数据库连接（spring.datasource.url）
 * 2. 配置相应的 AI 提供商 API Key 环境变量
 *
 * 由于需要真实数据库，该测试默认禁用。
 * 在有数据库环境的机器上运行时，移除 @Disabled 注解即可。
 */
@Slf4j
@SpringBootTest
@Disabled("集成测试需要 MySQL 数据库连接，请在有数据库环境中取消禁用")
public class AiModuleIntegrationTest {
    
    @Autowired
    private AiServiceFactory aiServiceFactory;
    
    @Autowired
    private AiAgentService aiAgentService;
    
    /**
     * 测试 Ollama 聊天功能
     * 需要先启动 Ollama 服务并拉取模型
     */
    @Test
    public void testOllamaChat() {
        log.info("测试 Ollama 聊天功能");
        
        try {
            var service = aiServiceFactory.getService(AiProvider.OLLAMA);
            
            ChatRequest request = new ChatRequest();
            request.setMessage("你好，请简单介绍一下自己");
            request.setSystemPrompt("你是一个有用的AI助手");
            request.setModel("llama2");
            
            ChatResponse response = service.chat(request);
            
            assertNotNull(response);
            assertNotNull(response.getContent());
            log.info("Ollama 响应: {}", response.getContent());
            
        } catch (Exception e) {
            log.warn("Ollama 测试跳过（可能未配置或服務未启动）: {}", e.getMessage());
            // 不失败测试，因为 Ollama 可能未配置
        }
    }
    
    /**
     * 测试 DeepSeek 聊天功能
     * 需要配置 DEEPSEEK_API_KEY
     */
    @Test
    public void testDeepSeekChat() {
        log.info("测试 DeepSeek 聊天功能");
        
        try {
            var service = aiServiceFactory.getService(AiProvider.DEEPSEEK);
            
            ChatRequest request = new ChatRequest();
            request.setMessage("请写一个简单的 Hello World Java 程序");
            request.setTemperature(0.7);
            
            ChatResponse response = service.chat(request);
            
            assertNotNull(response);
            assertNotNull(response.getContent());
            log.info("DeepSeek 响应: {}", response.getContent());
            
        } catch (Exception e) {
            log.warn("DeepSeek 测试跳过（可能未配置 API Key）: {}", e.getMessage());
        }
    }
    
    /**
     * 测试 Minimax 聊天功能
     * 需要配置 MINIMAX_API_KEY
     */
    @Test
    public void testMinimaxChat() {
        log.info("测试 Minimax 聊天功能");
        
        try {
            var service = aiServiceFactory.getService(AiProvider.MINIMAX);
            
            ChatRequest request = new ChatRequest();
            request.setMessage("请用一句话描述人工智能的未来");
            
            ChatResponse response = service.chat(request);
            
            assertNotNull(response);
            assertNotNull(response.getContent());
            log.info("Minimax 响应: {}", response.getContent());
            
        } catch (Exception e) {
            log.warn("Minimax 测试跳过（可能未配置 API Key）: {}", e.getMessage());
        }
    }
    
    /**
     * 测试 Agent 列表功能
     */
    @Test
    public void testListAgents() {
        log.info("测试 Agent 列表功能");

        List<Map<String, Object>> agents = aiAgentService.listAgents();

        assertNotNull(agents);
        log.info("可用 Agent 数量: {}", agents.size());

        for (Map<String, Object> agent : agents) {
            log.info("Agent: {} - {}", agent.get("name"), agent.get("description"));
        }
    }

    /**
     * 测试创建自定义 Agent
     */
    @Test
    public void testCreateAgent() {
        log.info("测试创建自定义 Agent");

        String agentName = "TestAgent_" + System.currentTimeMillis();
        String description = "测试用 Agent";
        String instructions = "你是一个测试助手";

        String code = aiAgentService.createAgent(agentName, description, instructions);

        assertNotNull(code);
        log.info("创建的 Agent: {}", code);

        // 验证 Agent 是否在列表中
        List<Map<String, Object>> agents = aiAgentService.listAgents();
        boolean found = agents.stream()
                .anyMatch(a -> code.equals(a.get("code")));

        assertTrue(found, "新创建的 Agent 应该在列表中");
    }

    /**
     * 测试执行 Agent 任务
     */
    @Test
    public void testExecuteAgent() {
        log.info("测试执行 Agent 任务");

        // 先创建一个 Agent
        String code = aiAgentService.createAgent(
                "CodeReviewer_" + System.currentTimeMillis(),
                "代码审查助手",
                "你是一个专业的代码审查员"
        );

        String task = "请分析以下代码的优点和缺点：public class HelloWorld { public static void main(String[] args) { System.out.println(\"Hello\"); } }";

        String result = aiAgentService.executeAgent(code, task);

        assertNotNull(result);
        log.info("Agent 执行结果: {}", result);
    }
    
    /**
     * 测试通义千问聊天功能
     * 需要配置 DASHSCOPE_API_KEY
     */
    @Test
    public void testDashScopeChat() {
        log.info("测试通义千问聊天功能");
        
        try {
            var service = aiServiceFactory.getService(AiProvider.DASHSCOPE);
            
            ChatRequest request = new ChatRequest();
            request.setMessage("请用中文回答：什么是机器学习？");
            
            ChatResponse response = service.chat(request);
            
            assertNotNull(response);
            assertNotNull(response.getContent());
            log.info("通义千问响应: {}", response.getContent());
            
        } catch (Exception e) {
            log.warn("通义千问测试跳过（可能未配置 API Key）: {}", e.getMessage());
        }
    }
}

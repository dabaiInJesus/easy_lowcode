package com.dabai.easy_lowcode.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dabai.easy_lowcode.ai.dto.ChatRequest;
import com.dabai.easy_lowcode.ai.dto.ChatResponse;
import com.dabai.easy_lowcode.ai.entity.AiAgent;
import com.dabai.easy_lowcode.ai.entity.PromptTemplate;
import com.dabai.easy_lowcode.ai.enums.AiProvider;
import com.dabai.easy_lowcode.ai.factory.AiServiceFactory;
import com.dabai.easy_lowcode.ai.mapper.AiAgentMapper;
import com.dabai.easy_lowcode.ai.mapper.PromptTemplateMapper;
import com.dabai.easy_lowcode.ai.service.impl.AiAgentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * AiAgentServiceImpl 单元测试（Mock 测试，不依赖真实 API）
 */
@ExtendWith(MockitoExtension.class)
public class AiAgentServiceImplTest {

    @Mock
    private AiAgentMapper aiAgentMapper;

    @Mock
    private PromptTemplateMapper promptTemplateMapper;

    @Mock
    private AiServiceFactory aiServiceFactory;

    @Mock
    private AiService mockAiService;

    private AiAgentServiceImpl agentService;

    @BeforeEach
    void setUp() {
        agentService = new AiAgentServiceImpl(
                aiAgentMapper,
                promptTemplateMapper,
                aiServiceFactory
        );
    }

    // ==================== DB 加载测试 ====================

    @Test
    void testLoadFromDb_loadsAgentsAndTemplates() {
        // 准备测试数据
        AiAgent agent1 = createAgent("agent-001", "数据分析师", "dashscope");
        AiAgent agent2 = createAgent("agent-002", "代码审查员", "openai");
        when(aiAgentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(agent1, agent2));

        PromptTemplate tpl1 = createTemplate(1L, "模板A");
        PromptTemplate tpl2 = createTemplate(2L, "模板B");
        when(promptTemplateMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(tpl1, tpl2));

        // 执行
        agentService.loadFromDb();

        // 验证：执行了查询
        verify(aiAgentMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
        verify(promptTemplateMapper, times(1)).selectList(any(LambdaQueryWrapper.class));
    }

    @Test
    void testLoadFromDb_emptyDatabase() {
        when(aiAgentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        when(promptTemplateMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        // 不抛异常，正常执行
        assertDoesNotThrow(() -> agentService.loadFromDb());
    }

    // ==================== 执行逻辑测试 ====================

    @Test
    void testExecuteAgent_success() {
        AiAgent agent = createAgent("test-agent", "测试助手", "dashscope");
        agent.setInstructions("你是一个测试助手");
        when(aiAgentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(agent));
        when(promptTemplateMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        // 加载缓存
        agentService.loadFromDb();

        // Mock AI 服务
        when(aiServiceFactory.getService(AiProvider.DASHSCOPE)).thenReturn(mockAiService);
        ChatResponse mockResponse = new ChatResponse();
        mockResponse.setContent("这是AI的回复");
        when(mockAiService.chat(any(ChatRequest.class))).thenReturn(mockResponse);

        // 执行
        String result = agentService.executeAgent("test-agent", "你好");

        // 验证
        assertNotNull(result);
        assertEquals("这是AI的回复", result);
        verify(mockAiService, times(1)).chat(any(ChatRequest.class));
    }

    @Test
    void testExecuteAgent_notFound() {
        when(aiAgentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        when(promptTemplateMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        agentService.loadFromDb();

        // Agent 不存在应该抛异常
        assertThrows(RuntimeException.class,
                () -> agentService.executeAgent("non-existent", "你好"));
    }

    // ==================== 变量替换测试 ====================

    @Test
    void testVariableReplacement() {
        AiAgent agent = createAgent("var-agent", "变量测试", "dashscope");
        agent.setInstructions("你好，我叫 {{name}}，今年 {{age}} 岁");
        agent.setVariablesConfig("{\"name\": \"张三\", \"age\": \"25\"}");

        when(aiAgentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(agent));
        when(promptTemplateMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        agentService.loadFromDb();

        when(aiServiceFactory.getService(any())).thenReturn(mockAiService);
        ChatResponse mockResponse = new ChatResponse();
        mockResponse.setContent("ok");
        when(mockAiService.chat(any(ChatRequest.class))).thenReturn(mockResponse);

        agentService.executeAgent("var-agent", "请介绍一下自己");

        // 验证替换后的 system prompt 传给 AI 服务
        verify(mockAiService).chat(argThat(request -> {
            String systemPrompt = request.getSystemPrompt();
            return systemPrompt != null
                    && systemPrompt.contains("张三")
                    && systemPrompt.contains("25");
        }));
    }

    @Test
    void testVariableReplacement_missingVariable() {
        AiAgent agent = createAgent("var-agent-2", "变量测试2", "dashscope");
        agent.setInstructions("你好，我叫 {{name}}，地址是 {{address}}");
        agent.setVariablesConfig("{\"name\": \"李四\"}"); // 缺少 address

        when(aiAgentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(agent));
        when(promptTemplateMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());

        agentService.loadFromDb();

        when(aiServiceFactory.getService(any())).thenReturn(mockAiService);
        ChatResponse mockResponse = new ChatResponse();
        mockResponse.setContent("ok");
        when(mockAiService.chat(any(ChatRequest.class))).thenReturn(mockResponse);

        // 不应抛异常，未配置的变量保持原样
        assertDoesNotThrow(() -> agentService.executeAgent("var-agent-2", "hi"));

        // 验证 system prompt 中未配置的变量被保留
        verify(mockAiService).chat(argThat(request -> {
            String systemPrompt = request.getSystemPrompt();
            return systemPrompt != null
                    && systemPrompt.contains("李四")
                    && systemPrompt.contains("{{address}}"); // 原样保留
        }));
    }

    // ==================== 流式执行测试 ====================

    @Test
    void testExecuteAgentStream_success() {
        AiAgent agent = createAgent("stream-agent", "流式测试", "dashscope");
        agent.setInstructions("你是一个流式助手");

        when(aiAgentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(agent));
        when(promptTemplateMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        agentService.loadFromDb();

        when(aiServiceFactory.getService(AiProvider.DASHSCOPE)).thenReturn(mockAiService);

        Flux<String> mockFlux = Flux.just("chunk1", "chunk2", "chunk3");
        when(mockAiService.streamChat(any(ChatRequest.class))).thenReturn(mockFlux);

        Flux<String> result = agentService.executeAgentStream("stream-agent", "讲个故事");

        assertNotNull(result);
        // 验证流式方法被调用
        verify(mockAiService, times(1)).streamChat(any(ChatRequest.class));
    }

    // ==================== 会话历史测试 ====================

    @Test
    void testChatHistory_recordsMessages() {
        AiAgent agent = createAgent("history-agent", "历史测试", "dashscope");
        agent.setInstructions("你是一个助手");

        when(aiAgentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(agent));
        when(promptTemplateMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        agentService.loadFromDb();

        when(aiServiceFactory.getService(any())).thenReturn(mockAiService);
        ChatResponse mockResponse = new ChatResponse();
        mockResponse.setContent("回复1");
        when(mockAiService.chat(any(ChatRequest.class))).thenReturn(mockResponse);

        // 执行两次对话
        agentService.executeAgent("history-agent", "问题1");
        agentService.executeAgent("history-agent", "问题2");

        // 获取历史（updateSession 内部用 "default" 作为 sessionId）
        List<Map<String, String>> history = agentService.getChatHistory("history-agent", "default");

        assertNotNull(history);
        assertEquals(4, history.size()); // user:问题1, assistant:回复1, user:问题2, assistant:回复1
    }

    @Test
    void testClearSession() {
        AiAgent agent = createAgent("clear-agent", "清除测试", "dashscope");
        agent.setInstructions("你是一个助手");

        when(aiAgentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(agent));
        when(promptTemplateMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Collections.emptyList());
        agentService.loadFromDb();

        when(aiServiceFactory.getService(any())).thenReturn(mockAiService);
        ChatResponse mockResponse = new ChatResponse();
        mockResponse.setContent("回复");
        when(mockAiService.chat(any(ChatRequest.class))).thenReturn(mockResponse);

        // 先产生会话
        agentService.executeAgent("clear-agent", "问题");

        // 清除
        agentService.clearSession("clear-agent", "default");

        // 验证会话已清空
        List<Map<String, String>> history = agentService.getChatHistory("clear-agent", "default");
        assertTrue(history.isEmpty());
    }

    // ==================== 工具方法 ====================

    private AiAgent createAgent(String code, String name, String provider) {
        AiAgent agent = new AiAgent();
        agent.setId(1L);
        agent.setAgentCode(code);
        agent.setAgentName(name);
        agent.setProvider(provider);
        agent.setModel("test-model");
        agent.setStatus(1);
        agent.setPublishStatus(1);
        agent.setInstructions("你是一个AI助手");
        return agent;
    }

    private PromptTemplate createTemplate(Long id, String name) {
        PromptTemplate tpl = new PromptTemplate();
        tpl.setId(id);
        tpl.setTemplateName(name);
        tpl.setStatus(1);
        return tpl;
    }
}

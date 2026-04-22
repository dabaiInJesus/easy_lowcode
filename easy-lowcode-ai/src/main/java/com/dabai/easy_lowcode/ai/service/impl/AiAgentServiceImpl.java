package com.dabai.easy_lowcode.ai.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabai.easy_lowcode.ai.dto.ChatRequest;
import com.dabai.easy_lowcode.ai.dto.ChatResponse;
import com.dabai.easy_lowcode.ai.entity.AiAgent;
import com.dabai.easy_lowcode.ai.entity.PromptTemplate;
import com.dabai.easy_lowcode.ai.enums.AiProvider;
import com.dabai.easy_lowcode.ai.factory.AiServiceFactory;
import com.dabai.easy_lowcode.ai.mapper.AiAgentMapper;
import com.dabai.easy_lowcode.ai.service.AiAgentService;
import com.dabai.easy_lowcode.ai.service.AiService;
import com.dabai.easy_lowcode.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 智能体服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAgentServiceImpl extends ServiceImpl<AiAgentMapper, AiAgent> implements AiAgentService {
    
    private final AiServiceFactory aiServiceFactory;
    
    @Override
    public ChatResponse executeAgent(String agentCode, ChatRequest request) {
        log.info("执行智能体: {}, 消息: {}", agentCode, request.getMessage());
        
        // 1. 查询智能体
        LambdaQueryWrapper<AiAgent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiAgent::getAgentCode, agentCode);
        wrapper.eq(AiAgent::getStatus, 1);
        AiAgent agent = this.getOne(wrapper);
        
        if (agent == null) {
            throw new BusinessException("智能体不存在或已禁用");
        }
        
        if (agent.getPublishStatus() != 1) {
            throw new BusinessException("智能体未发布");
        }
        
        // 2. 构建聊天请求
        ChatRequest agentRequest = buildChatRequest(agent, request);
        
        // 3. 获取对应的 AI 服务
        AiProvider provider = AiProvider.fromCode(agent.getProvider());
        AiService aiService = aiServiceFactory.getService(provider);
        
        // 4. 执行对话
        ChatResponse response;
        if (agent.getEnableWorkflow() == 1) {
            // TODO: 执行工作流
            log.warn("工作流功能待实现，使用普通对话");
            response = aiService.chat(agentRequest);
        } else {
            response = aiService.chat(agentRequest);
        }
        
        // 5. 增加使用次数
        this.incrementUsageCount(agent.getId());
        
        log.info("智能体执行成功: {}", agentCode);
        return response;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publishAgent(Long agentId) {
        AiAgent agent = this.getById(agentId);
        if (agent == null) {
            throw new BusinessException("智能体不存在");
        }
        
        agent.setPublishStatus(1);
        this.updateById(agent);
        
        log.info("智能体发布成功: {}", agentId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementUsageCount(Long agentId) {
        AiAgent agent = this.getById(agentId);
        if (agent != null) {
            agent.setUsageCount(agent.getUsageCount() + 1);
            this.updateById(agent);
        }
    }
    
    /**
     * 构建聊天请求
     */
    private ChatRequest buildChatRequest(AiAgent agent, ChatRequest userRequest) {
        ChatRequest request = new ChatRequest();
        
        // 设置模型参数
        request.setModel(agent.getModel());
        request.setTemperature(agent.getTemperature());
        request.setMaxTokens(agent.getMaxTokens());
        request.setMessage(userRequest.getMessage());
        
        // 处理提示词模板
        if (agent.getPromptTemplateId() != null) {
            // TODO: 从数据库查询提示词模板并替换变量
            String systemPrompt = renderPromptTemplate(agent.getPromptTemplateId(), userRequest);
            request.setSystemPrompt(systemPrompt);
        }
        
        return request;
    }
    
    /**
     * 渲染提示词模板
     */
    private String renderPromptTemplate(Long templateId, ChatRequest request) {
        // TODO: 实现提示词模板渲染逻辑
        // 这里应该查询 PromptTemplate 表，然后替换 {{variable}} 变量
        return "你是一个有用的AI助手";
    }
}

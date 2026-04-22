package com.dabai.easy_lowcode.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.ai.dto.ChatRequest;
import com.dabai.easy_lowcode.ai.dto.ChatResponse;
import com.dabai.easy_lowcode.ai.entity.AiAgent;
import com.dabai.easy_lowcode.ai.service.AiAgentService;
import com.dabai.easy_lowcode.common.result.PageResult;
import com.dabai.easy_lowcode.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 智能体控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/agent")
@RequiredArgsConstructor
public class AiAgentController {
    
    private final AiAgentService agentService;
    
    /**
     * 分页查询智能体列表
     */
    @GetMapping("/page")
    public Result<PageResult<AiAgent>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword) {
        
        LambdaQueryWrapper<AiAgent> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(AiAgent::getAgentName, keyword)
                   .or()
                   .like(AiAgent::getDescription, keyword);
        }
        wrapper.orderByDesc(AiAgent::getCreateTime);
        
        Page<AiAgent> page = agentService.page(new Page<>(current, size), wrapper);
        
        PageResult<AiAgent> result = new PageResult<>(
            page.getTotal(),
            page.getCurrent(),
            page.getSize(),
            page.getRecords()
        );
        
        return Result.success(result);
    }
    
    /**
     * 获取智能体详情
     */
    @GetMapping("/{id}")
    public Result<AiAgent> getById(@PathVariable Long id) {
        AiAgent agent = agentService.getById(id);
        return Result.success(agent);
    }
    
    /**
     * 根据编码获取智能体
     */
    @GetMapping("/code/{code}")
    public Result<AiAgent> getByCode(@PathVariable String code) {
        LambdaQueryWrapper<AiAgent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiAgent::getAgentCode, code);
        AiAgent agent = agentService.getOne(wrapper);
        return Result.success(agent);
    }
    
    /**
     * 创建智能体
     */
    @PostMapping
    public Result<Void> create(@RequestBody AiAgent agent) {
        agent.setPublishStatus(0); // 默认草稿状态
        agent.setUsageCount(0);
        agentService.save(agent);
        return Result.success();
    }
    
    /**
     * 更新智能体
     */
    @PutMapping
    public Result<Void> update(@RequestBody AiAgent agent) {
        agentService.updateById(agent);
        return Result.success();
    }
    
    /**
     * 删除智能体
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        agentService.removeById(id);
        return Result.success();
    }
    
    /**
     * 发布智能体
     */
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        agentService.publishAgent(id);
        return Result.success("发布成功");
    }
    
    /**
     * 执行智能体对话
     */
    @PostMapping("/{code}/chat")
    public Result<ChatResponse> chat(@PathVariable String code, @RequestBody ChatRequest request) {
        try {
            ChatResponse response = agentService.executeAgent(code, request);
            return Result.success(response);
        } catch (Exception e) {
            log.error("智能体对话失败", e);
            return Result.error("对话失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取已发布的智能体列表
     */
    @GetMapping("/published")
    public Result<List<AiAgent>> getPublishedAgents() {
        LambdaQueryWrapper<AiAgent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AiAgent::getStatus, 1);
        wrapper.eq(AiAgent::getPublishStatus, 1);
        wrapper.orderByDesc(AiAgent::getUsageCount);
        
        List<AiAgent> agents = agentService.list(wrapper);
        return Result.success(agents);
    }
}

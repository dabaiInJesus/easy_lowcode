package com.dabai.easy_lowcode.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.ai.config.AiConfigRefresher;
import com.dabai.easy_lowcode.ai.entity.AiConfig;
import com.dabai.easy_lowcode.ai.mapper.AiConfigMapper;
import com.dabai.easy_lowcode.common.result.PageResult;
import com.dabai.easy_lowcode.common.result.Result;
import com.dabai.easy_lowcode.common.util.EncryptUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI供应商配置控制器（支持前端UI管理AI配置）
 */
@Tag(name = "AI配置管理", description = "AI供应商配置的CRUD管理")
@Slf4j
@RestController
@RequestMapping("/api/ai/config")
@RequiredArgsConstructor
public class AiConfigController {

    private final AiConfigMapper aiConfigMapper;
    private final AiConfigRefresher aiConfigRefresher;

    @Operation(summary = "分页查询AI配置列表", description = "分页查询所有AI供应商配置")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/page")
    public Result<PageResult<AiConfig>> page(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size) {
        Page<AiConfig> page = aiConfigMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<AiConfig>().orderByAsc(AiConfig::getSortOrder));
        page.getRecords().forEach(c -> {
            if (c.getApiKey() != null && c.getApiKey().length() > 8) {
                c.setApiKey(c.getApiKey().substring(0, 4) + "****" + c.getApiKey().substring(c.getApiKey().length() - 4));
            }
        });
        return Result.success(new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @Operation(summary = "保存AI配置", description = "创建新的AI供应商配置")
    @ApiResponse(responseCode = "200", description = "保存成功")
    @PostMapping
    @Transactional
    public Result<Void> save(@RequestBody AiConfig config) {
        if (config.getApiKey() != null && !config.getApiKey().contains("****")) {
            try { config.setApiKey(EncryptUtil.encrypt(config.getApiKey())); } catch (Exception e) {
                log.warn("API Key加密失败: {}", e.getMessage());
            }
        }
        if (config.getSecretKey() != null && !config.getSecretKey().contains("****")) {
            try { config.setSecretKey(EncryptUtil.encrypt(config.getSecretKey())); } catch (Exception e) {
                log.warn("Secret Key加密失败: {}", e.getMessage());
            }
        }
        if (config.getIsDefault() == 1) {
            aiConfigMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AiConfig>()
                    .set(AiConfig::getIsDefault, 0).eq(AiConfig::getIsDefault, 1));
        }
        aiConfigMapper.insert(config);
        aiConfigRefresher.refreshConfig(config);
        return Result.success("配置已保存");
    }

    @Operation(summary = "更新AI配置", description = "更新AI供应商配置信息")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping
    @Transactional
    public Result<Void> update(@RequestBody AiConfig config) {
        AiConfig existing = aiConfigMapper.selectById(config.getId());
        if (config.getApiKey() != null && !config.getApiKey().contains("****")) {
            try { config.setApiKey(EncryptUtil.encrypt(config.getApiKey())); } catch (Exception e) {
                log.warn("API Key加密失败: {}", e.getMessage());
            }
        } else { config.setApiKey(existing != null ? existing.getApiKey() : null); }
        if (config.getSecretKey() != null && !config.getSecretKey().contains("****")) {
            try { config.setSecretKey(EncryptUtil.encrypt(config.getSecretKey())); } catch (Exception e) {
                log.warn("Secret Key加密失败: {}", e.getMessage());
            }
        } else { config.setSecretKey(existing != null ? existing.getSecretKey() : null); }
        if (config.getIsDefault() == 1) {
            aiConfigMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AiConfig>()
                    .set(AiConfig::getIsDefault, 0).eq(AiConfig::getIsDefault, 1));
        }
        aiConfigMapper.updateById(config);
        aiConfigRefresher.refreshConfig(config);
        return Result.success("配置已更新");
    }

    @Operation(summary = "删除AI配置", description = "删除AI供应商配置")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "配置ID") @PathVariable Long id) {
        aiConfigMapper.deleteById(id);
        return Result.success("配置已删除");
    }

    @Operation(summary = "获取AI配置列表", description = "获取所有已启用的AI配置（脱敏）")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/list")
    public Result<List<AiConfig>> list() {
        List<AiConfig> list = aiConfigMapper.selectList(
                new LambdaQueryWrapper<AiConfig>().eq(AiConfig::getStatus, 1)
                        .orderByAsc(AiConfig::getSortOrder));
        list.forEach(c -> c.setApiKey(null));
        return Result.success(list);
    }
}

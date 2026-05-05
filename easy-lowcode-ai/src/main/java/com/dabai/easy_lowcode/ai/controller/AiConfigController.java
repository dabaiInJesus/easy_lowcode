package com.dabai.easy_lowcode.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.ai.entity.AiConfig;
import com.dabai.easy_lowcode.ai.mapper.AiConfigMapper;
import com.dabai.easy_lowcode.common.result.PageResult;
import com.dabai.easy_lowcode.common.result.Result;
import com.dabai.easy_lowcode.common.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI供应商配置控制器（支持前端UI管理AI配置）
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/config")
@RequiredArgsConstructor
public class AiConfigController {

    private final AiConfigMapper aiConfigMapper;

    @GetMapping("/page")
    public Result<PageResult<AiConfig>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size) {
        Page<AiConfig> page = aiConfigMapper.selectPage(
                new Page<>(current, size),
                new LambdaQueryWrapper<AiConfig>().orderByAsc(AiConfig::getSortOrder));
        // 脱敏API Key
        page.getRecords().forEach(c -> {
            if (c.getApiKey() != null && c.getApiKey().length() > 8) {
                c.setApiKey(c.getApiKey().substring(0, 4) + "****" + c.getApiKey().substring(c.getApiKey().length() - 4));
            }
        });
        return Result.success(new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()));
    }

    @PostMapping
    public Result<Void> save(@RequestBody AiConfig config) {
        if (config.getApiKey() != null && !config.getApiKey().contains("****")) {
            try { config.setApiKey(EncryptUtil.encrypt(config.getApiKey())); } catch (Exception ignored) {}
        }
        if (config.getSecretKey() != null && !config.getSecretKey().contains("****")) {
            try { config.setSecretKey(EncryptUtil.encrypt(config.getSecretKey())); } catch (Exception ignored) {}
        }
        if (config.getIsDefault() == 1) {
            aiConfigMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AiConfig>()
                    .set(AiConfig::getIsDefault, 0).eq(AiConfig::getIsDefault, 1));
        }
        aiConfigMapper.insert(config);
        return Result.success("配置已保存");
    }

    @PutMapping
    public Result<Void> update(@RequestBody AiConfig config) {
        if (config.getApiKey() != null && !config.getApiKey().contains("****")) {
            try { config.setApiKey(EncryptUtil.encrypt(config.getApiKey())); } catch (Exception ignored) {}
        } else { config.setApiKey(null); }
        if (config.getSecretKey() != null && !config.getSecretKey().contains("****")) {
            try { config.setSecretKey(EncryptUtil.encrypt(config.getSecretKey())); } catch (Exception ignored) {}
        } else { config.setSecretKey(null); }
        if (config.getIsDefault() == 1) {
            aiConfigMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<AiConfig>()
                    .set(AiConfig::getIsDefault, 0).eq(AiConfig::getIsDefault, 1));
        }
        aiConfigMapper.updateById(config);
        return Result.success("配置已更新");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        aiConfigMapper.deleteById(id);
        return Result.success("配置已删除");
    }

    @GetMapping("/list")
    public Result<List<AiConfig>> list() {
        List<AiConfig> list = aiConfigMapper.selectList(
                new LambdaQueryWrapper<AiConfig>().eq(AiConfig::getStatus, 1)
                        .orderByAsc(AiConfig::getSortOrder));
        list.forEach(c -> c.setApiKey(null));
        return Result.success(list);
    }
}

package com.dabai.easy_lowcode.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dabai.easy_lowcode.auth.entity.SysMenu;
import com.dabai.easy_lowcode.auth.service.SysMenuService;
import com.dabai.easy_lowcode.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth/menu")
@RequiredArgsConstructor
public class MenuController {
    
    private final SysMenuService menuService;
    
    /**
     * 获取菜单树形列表
     */
    @GetMapping("/list")
    public Result<List<SysMenu>> getMenuList() {
        List<SysMenu> menuTree = menuService.getMenuTree();
        return Result.success(menuTree);
    }
    
    /**
     * 创建菜单
     */
    @PostMapping
    public Result<Void> createMenu(@RequestBody SysMenu menu) {
        // 自动生成menu_code（如果未提供）
        if (menu.getMenuCode() == null || menu.getMenuCode().trim().isEmpty()) {
            menu.setMenuCode(generateMenuCode(menu.getMenuName()));
        }
        
        // 验证menu_code唯一性
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getMenuCode, menu.getMenuCode());
        if (menuService.count(wrapper) > 0) {
            return Result.error("菜单编码已存在");
        }
        
        menuService.save(menu);
        return Result.success();
    }
    
    /**
     * 更新菜单
     */
    @PutMapping
    public Result<Void> updateMenu(@RequestBody SysMenu menu) {
        // 验证menu_code唯一性（排除自身）
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getMenuCode, menu.getMenuCode())
               .ne(SysMenu::getId, menu.getId());
        if (menuService.count(wrapper) > 0) {
            return Result.error("菜单编码已存在");
        }
        
        menuService.updateById(menu);
        return Result.success();
    }
    
    /**
     * 删除菜单
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteMenu(@PathVariable Long id) {
        // 检查是否有子菜单
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getParentId, id);
        long count = menuService.count(wrapper);
        if (count > 0) {
            return Result.error("该菜单下有子菜单，无法删除");
        }
        
        menuService.removeById(id);
        return Result.success();
    }
    
    /**
     * 根据菜单名称生成菜单编码
     */
    private String generateMenuCode(String menuName) {
        if (menuName == null || menuName.trim().isEmpty()) {
            return "menu_" + System.currentTimeMillis();
        }
        
        // 中文转拼音的逻辑可以后续实现，这里先用简单规则
        // 将中文转换为拼音首字母或下划线格式
        String code = menuName.replaceAll("[\\s\\u3000]+", "_")  // 替换空格
                             .replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "")  // 移除特殊字符
                             .toLowerCase();
        
        // 如果是中文，使用拼音库转换（暂时用简单方案）
        if (code.matches(".*[\\u4e00-\\u9fa5].*")) {
            // 简单方案：使用中文名称的拼音首字母
            // TODO: 后续可以集成pinyin4j库进行完整转换
            code = convertChineseToPinyin(code);
        }
        
        // 确保不以数字开头
        if (code.matches("^\\d.*")) {
            code = "menu_" + code;
        }
        
        return code;
    }
    
    /**
     * 简单的中文转拼音方法（临时方案）
     */
    private String convertChineseToPinyin(String chinese) {
        // 这是一个简化的实现
        // 实际项目中建议使用 pinyin4j 或其他拼音库
        StringBuilder result = new StringBuilder();
        for (char c : chinese.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fa5') {
                // 对于中文字符，保留原字符（后续可以用拼音库替换）
                result.append(c);
            } else {
                result.append(c);
            }
        }
        // 返回原始字符串，建议用户手动输入英文menu_code
        return result.toString();
    }
}

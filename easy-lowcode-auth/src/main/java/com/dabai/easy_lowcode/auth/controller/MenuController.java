package com.dabai.easy_lowcode.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dabai.easy_lowcode.auth.entity.SysMenu;
import com.dabai.easy_lowcode.auth.service.SysMenuService;
import com.dabai.easy_lowcode.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理控制器
 */
@Tag(name = "菜单管理", description = "菜单CRUD及树形结构管理")
@Slf4j
@RestController
@RequestMapping("/auth/menu")
@RequiredArgsConstructor
public class MenuController {
    
    private final SysMenuService menuService;
    
    @Operation(summary = "获取菜单树形列表", description = "获取树形结构的菜单列表")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/list")
    public Result<List<SysMenu>> getMenuList() {
        List<SysMenu> menuTree = menuService.getMenuTree();
        return Result.success(menuTree);
    }
    
    @Operation(summary = "创建菜单", description = "创建新菜单，自动生成菜单编码")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PostMapping
    public Result<Void> createMenu(@RequestBody SysMenu menu) {
        if (menu.getMenuCode() == null || menu.getMenuCode().trim().isEmpty()) {
            menu.setMenuCode(generateMenuCode(menu.getMenuName()));
        }
        
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getMenuCode, menu.getMenuCode());
        if (menuService.count(wrapper) > 0) {
            return Result.error("菜单编码已存在");
        }
        
        menuService.save(menu);
        return Result.success();
    }
    
    @Operation(summary = "更新菜单", description = "更新菜单信息")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping
    public Result<Void> updateMenu(@RequestBody SysMenu menu) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getMenuCode, menu.getMenuCode())
               .ne(SysMenu::getId, menu.getId());
        if (menuService.count(wrapper) > 0) {
            return Result.error("菜单编码已存在");
        }
        
        menuService.updateById(menu);
        return Result.success();
    }
    
    @Operation(summary = "删除菜单", description = "删除菜单（需确保无子菜单）")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/{id}")
    public Result<Void> deleteMenu(@Parameter(description = "菜单ID") @PathVariable Long id) {
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysMenu::getParentId, id);
        long count = menuService.count(wrapper);
        if (count > 0) {
            return Result.error("该菜单下有子菜单，无法删除");
        }
        
        menuService.removeById(id);
        return Result.success();
    }
    
    private String generateMenuCode(String menuName) {
        if (menuName == null || menuName.trim().isEmpty()) {
            return "menu_" + System.currentTimeMillis();
        }
        
        String code = menuName.replaceAll("[\\s\\u3000]+", "_")
                              .replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "")
                              .toLowerCase();
        
        if (code.matches(".*[\\u4e00-\\u9fa5].*")) {
            code = convertChineseToPinyin(code);
        }
        
        if (code.matches("^\\d.*")) {
            code = "menu_" + code;
        }
        
        return code;
    }
    
    private String convertChineseToPinyin(String chinese) {
        StringBuilder result = new StringBuilder();
        for (char c : chinese.toCharArray()) {
            if (c >= '\u4e00' && c <= '\u9fa5') {
                result.append(c);
            } else {
                result.append(c);
            }
        }
        return result.toString();
    }
}

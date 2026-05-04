package com.dabai.easy_lowcode.auth.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.auth.entity.SysUser;
import com.dabai.easy_lowcode.auth.service.SysUserService;
import com.dabai.easy_lowcode.common.result.PageResult;
import com.dabai.easy_lowcode.common.result.Result;
import com.dabai.easy_lowcode.common.util.EncryptUtil;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final SysUserService userService;
    private final com.dabai.easy_lowcode.auth.mapper.SysRoleMapper roleMapper;
    private final com.dabai.easy_lowcode.auth.mapper.SysMenuMapper menuMapper;
    private final com.dabai.easy_lowcode.auth.mapper.SysDeptMapper deptMapper;
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        String token = userService.login(request.getUsername(), request.getPassword());
        
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        
        return Result.success("登录成功", data);
    }
    
    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.success("登出成功", null);
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/current")
    public Result<SysUser> getCurrentUser() {
        SysUser user = userService.getCurrentUser();
        return Result.success(user);
    }
    
    /**
     * 分页查询用户列表
     */
    @GetMapping("/user/page")
    public Result<PageResult<SysUser>> pageUsers(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword) {
        
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysUser::getUsername, keyword)
                   .or()
                   .like(SysUser::getRealName, keyword);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        
        Page<SysUser> page = userService.page(new Page<>(current, size), wrapper);
        
        PageResult<SysUser> result = new PageResult<>(
            page.getTotal(),
            page.getCurrent(),
            page.getSize(),
            page.getRecords()
        );
        
        return Result.success(result);
    }
    
    /**
     * 获取用户详情
     */
    @GetMapping("/user/{id}")
    public Result<SysUser> getUserById(@PathVariable Long id) {
        SysUser user = userService.getById(id);
        // 不返回密码
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }
    
    /**
     * 创建用户
     */
    @PostMapping("/user")
    public Result<Void> createUser(@RequestBody SysUser user) {
        // 加密密码
        if (user.getPassword() != null) {
            user.setPassword(EncryptUtil.md5(user.getPassword()));
        }
        userService.save(user);
        return Result.success("创建成功");
    }
    
    /**
     * 更新用户
     */
    @PutMapping("/user")
    public Result<Void> updateUser(@RequestBody SysUser user) {
        // 不允许直接修改密码
        user.setPassword(null);
        userService.updateById(user);
        return Result.success("更新成功");
    }
    
    /**
     * 删除用户
     */
    @DeleteMapping("/user/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.removeById(id);
        return Result.success("删除成功");
    }
    
    /**
     * 重置密码
     */
    @PostMapping("/user/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id, @RequestBody Map<String, String> params) {
        String newPassword = params.get("newPassword");
        SysUser user = new SysUser();
        user.setId(id);
        // 使用 BCrypt 加密密码
        user.setPassword(EncryptUtil.bcrypt(newPassword));
        userService.updateById(user);
        return Result.success("密码重置成功");
    }
    
    /**
     * 临时接口：生成BCrypt密码哈希（仅用于测试）
     */
    @GetMapping("/generate-bcrypt")
    public Result<Map<String, String>> generateBcrypt(@RequestParam String password) {
        String bcryptHash = EncryptUtil.bcrypt(password);
        Map<String, String> data = new HashMap<>();
        data.put("password", password);
        data.put("bcryptHash", bcryptHash);
        return Result.success(data);
    }
    
    /**
     * 临时接口：修复admin用户密码为BCrypt格式
     */
    @PostMapping("/fix-admin-password")
    public Result<Map<String, String>> fixAdminPassword() {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, "admin");
        SysUser admin = userService.getOne(wrapper);
        
        if (admin != null) {
            // 生成 BCrypt 密码
            String bcryptPassword = EncryptUtil.bcrypt("admin123");
            admin.setPassword(bcryptPassword);
            userService.updateById(admin);
            
            Map<String, String> data = new HashMap<>();
            data.put("message", "密码已修复");
            data.put("username", "admin");
            data.put("newPassword", "admin123");
            data.put("bcryptHash", bcryptPassword);
            return Result.success(data);
        } else {
            return Result.error("未找到 admin 用户");
        }
    }
    
    /**
     * 修改密码
     */
    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestBody Map<String, String> params) {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        
        Long userId = StpUtil.getLoginIdAsLong();
        SysUser user = userService.getById(userId);
        
        if (!EncryptUtil.verifyPassword(oldPassword, user.getPassword())) {
            return Result.error("原密码错误");
        }
        
        user.setPassword(EncryptUtil.bcrypt(newPassword));
        userService.updateById(user);
        
        return Result.success("密码修改成功");
    }
    
    /**
     * 登录请求DTO
     */
    @lombok.Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
    
    /**
     * 获取统计数据
     */
    @GetMapping("/statistics")
    public Result<Map<String, Long>> getStatistics() {
        Map<String, Long> statistics = new HashMap<>();
        
        // 用户总数
        long userCount = userService.count();
        statistics.put("userCount", userCount);
        
        // 角色总数
        long roleCount = roleMapper.selectCount(null);
        statistics.put("roleCount", roleCount);
        
        // 菜单总数
        long menuCount = menuMapper.selectCount(null);
        statistics.put("menuCount", menuCount);
        
        // 部门总数
        long deptCount = deptMapper.selectCount(null);
        statistics.put("deptCount", deptCount);
        
        return Result.success(statistics);
    }
    
    /**
     * 获取菜单列表
     */
    @GetMapping("/menu/list")
    public Result<List<com.dabai.easy_lowcode.auth.entity.SysMenu>> getMenuList() {
        List<com.dabai.easy_lowcode.auth.entity.SysMenu> menuList = menuMapper.selectList(null);
        return Result.success(menuList);
    }
    
    /**
     * 创建菜单
     */
    @PostMapping("/menu")
    public Result<Void> createMenu(@RequestBody com.dabai.easy_lowcode.auth.entity.SysMenu menu) {
        menuMapper.insert(menu);
        return Result.success("创建成功");
    }
    
    /**
     * 更新菜单
     */
    @PutMapping("/menu")
    public Result<Void> updateMenu(@RequestBody com.dabai.easy_lowcode.auth.entity.SysMenu menu) {
        menuMapper.updateById(menu);
        return Result.success("更新成功");
    }
    
    /**
     * 删除菜单
     */
    @DeleteMapping("/menu/{id}")
    public Result<Void> deleteMenu(@PathVariable Long id) {
        menuMapper.deleteById(id);
        return Result.success("删除成功");
    }
    
    /**
     * 获取部门列表
     */
    @GetMapping("/dept/list")
    public Result<List<com.dabai.easy_lowcode.auth.entity.SysDept>> getDeptList() {
        List<com.dabai.easy_lowcode.auth.entity.SysDept> deptList = deptMapper.selectList(null);
        return Result.success(deptList);
    }
    
    /**
     * 创建部门
     */
    @PostMapping("/dept")
    public Result<Void> createDept(@RequestBody com.dabai.easy_lowcode.auth.entity.SysDept dept) {
        deptMapper.insert(dept);
        return Result.success("创建成功");
    }
    
    /**
     * 更新部门
     */
    @PutMapping("/dept")
    public Result<Void> updateDept(@RequestBody com.dabai.easy_lowcode.auth.entity.SysDept dept) {
        deptMapper.updateById(dept);
        return Result.success("更新成功");
    }
    
    /**
     * 删除部门
     */
    @DeleteMapping("/dept/{id}")
    public Result<Void> deleteDept(@PathVariable Long id) {
        deptMapper.deleteById(id);
        return Result.success("删除成功");
    }
}

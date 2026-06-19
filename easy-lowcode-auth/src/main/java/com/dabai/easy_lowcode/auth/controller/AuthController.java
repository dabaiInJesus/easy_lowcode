package com.dabai.easy_lowcode.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.auth.entity.SysUser;
import com.dabai.easy_lowcode.auth.service.SysUserService;
import com.dabai.easy_lowcode.common.result.PageResult;
import com.dabai.easy_lowcode.common.result.Result;
import com.dabai.easy_lowcode.common.util.EncryptUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 认证控制器
 */
@Tag(name = "认证管理", description = "用户登录、登出、用户CRUD及密码管理")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final SysUserService userService;
    private final com.dabai.easy_lowcode.auth.mapper.SysRoleMapper roleMapper;
    private final com.dabai.easy_lowcode.auth.mapper.SysMenuMapper menuMapper;
    private final com.dabai.easy_lowcode.auth.mapper.SysDeptMapper deptMapper;
    
    @Operation(summary = "用户登录", description = "使用用户名和密码登录系统，返回token")
    @ApiResponse(responseCode = "200", description = "登录成功")
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        String token = userService.login(request.getUsername(), request.getPassword());
        
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        
        return Result.success("登录成功", data);
    }
    
    @Operation(summary = "用户登出", description = "退出当前登录")
    @ApiResponse(responseCode = "200", description = "登出成功")
    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.success("登出成功", null);
    }
    
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/current")
    public Result<SysUser> getCurrentUser() {
        SysUser user = userService.getCurrentUser();
        return Result.success(user);
    }
    
    @Operation(summary = "分页查询用户列表", description = "分页查询用户列表，支持关键词搜索")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/user/page")
    public Result<PageResult<SysUser>> pageUsers(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "搜索关键词（用户名或真实姓名）") @RequestParam(required = false) String keyword) {
        
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysUser::getUsername, keyword)
                   .or()
                   .like(SysUser::getRealName, keyword);
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        
        Page<SysUser> page = userService.page(new Page<>(current, size), wrapper);
        
        PageResult<SysUser> result = new PageResult<>(
            page.getRecords(),
            page.getTotal(),
            page.getCurrent(),
            page.getSize()
        );
        
        return Result.success(result);
    }
    
    @Operation(summary = "获取用户详情", description = "根据ID获取用户详细信息")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/user/{id}")
    public Result<SysUser> getUserById(@Parameter(description = "用户ID") @PathVariable Long id) {
        SysUser user = userService.getById(id);
        if (user != null) {
            user.setPassword(null);
        }
        return Result.success(user);
    }
    
    @Operation(summary = "创建用户", description = "创建新用户，密码会自动使用BCrypt加密")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PostMapping("/user")
    public Result<Void> createUser(@RequestBody SysUser user) {
        if (user.getPassword() != null) {
            user.setPassword(EncryptUtil.bcrypt(user.getPassword()));
        }
        userService.save(user);
        return Result.success("创建成功");
    }
    
    @Operation(summary = "更新用户", description = "更新用户信息（不允许修改密码）")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping("/user")
    public Result<Void> updateUser(@RequestBody SysUser user) {
        user.setPassword(null);
        userService.updateById(user);
        return Result.success("更新成功");
    }
    
    @Operation(summary = "删除用户", description = "根据ID删除用户")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/user/{id}")
    public Result<Void> deleteUser(@Parameter(description = "用户ID") @PathVariable Long id) {
        userService.removeById(id);
        return Result.success("删除成功");
    }
    
    @Operation(summary = "重置密码", description = "管理员重置指定用户的密码")
    @ApiResponse(responseCode = "200", description = "密码重置成功")
    @PostMapping("/user/{id}/reset-password")
    public Result<Void> resetPassword(
            @Parameter(description = "用户ID") @PathVariable Long id,
            @RequestBody Map<String, String> params) {
        String newPassword = params.get("newPassword");
        SysUser user = new SysUser();
        user.setId(id);
        user.setPassword(EncryptUtil.bcrypt(newPassword));
        userService.updateById(user);
        return Result.success("密码重置成功");
    }

    @Operation(summary = "修改密码", description = "当前用户修改自己的密码")
    @ApiResponse(responseCode = "200", description = "密码修改成功")
    @PostMapping("/change-password")
    public Result<Void> changePassword(@RequestBody Map<String, String> params) {
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        
        SysUser currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            return Result.error("用户未登录");
        }
        
        if (!EncryptUtil.verifyPassword(oldPassword, currentUser.getPassword())) {
            return Result.error("原密码错误");
        }
        
        currentUser.setPassword(EncryptUtil.bcrypt(newPassword));
        userService.updateById(currentUser);
        
        return Result.success("密码修改成功");
    }
    
    @lombok.Data
    public static class LoginRequest {
        @NotBlank(message = "用户名不能为空")
        private String username;
        @NotBlank(message = "密码不能为空")
        private String password;
    }
    
    @Operation(summary = "获取统计数据", description = "获取用户、角色、菜单、部门的统计数量")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/statistics")
    public Result<Map<String, Long>> getStatistics() {
        Map<String, Long> statistics = new HashMap<>();
        
        long userCount = userService.count();
        statistics.put("userCount", userCount);
        
        long roleCount = roleMapper.selectCount(null);
        statistics.put("roleCount", roleCount);
        
        long menuCount = menuMapper.selectCount(null);
        statistics.put("menuCount", menuCount);
        
        long deptCount = deptMapper.selectCount(null);
        statistics.put("deptCount", deptCount);
        
        return Result.success(statistics);
    }
    
    @Operation(summary = "获取部门列表", description = "获取所有部门列表")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/dept/list")
    public Result<List<com.dabai.easy_lowcode.auth.entity.SysDept>> getDeptList() {
        List<com.dabai.easy_lowcode.auth.entity.SysDept> deptList = deptMapper.selectList(null);
        return Result.success(deptList);
    }
    
    @Operation(summary = "创建部门", description = "创建新部门")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PostMapping("/dept")
    public Result<Void> createDept(@RequestBody com.dabai.easy_lowcode.auth.entity.SysDept dept) {
        deptMapper.insert(dept);
        return Result.success("创建成功");
    }
    
    @Operation(summary = "更新部门", description = "更新部门信息")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping("/dept")
    public Result<Void> updateDept(@RequestBody com.dabai.easy_lowcode.auth.entity.SysDept dept) {
        deptMapper.updateById(dept);
        return Result.success("更新成功");
    }
    
    @Operation(summary = "删除部门", description = "根据ID删除部门")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/dept/{id}")
    public Result<Void> deleteDept(@Parameter(description = "部门ID") @PathVariable Long id) {
        deptMapper.deleteById(id);
        return Result.success("删除成功");
    }
}

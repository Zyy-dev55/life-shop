package com.lifeshop.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.lifeshop.common.BusinessException;
import com.lifeshop.common.PageResult;
import com.lifeshop.dto.AdminUserCreateDTO;
import com.lifeshop.dto.LoginDTO;
import com.lifeshop.dto.PasswordDTO;
import com.lifeshop.dto.RegisterDTO;
import com.lifeshop.dto.UserUpdateDTO;
import com.lifeshop.entity.Merchant;
import com.lifeshop.entity.User;
import com.lifeshop.mapper.MerchantMapper;
import com.lifeshop.mapper.UserMapper;
import com.lifeshop.utils.JwtUtil;
import com.lifeshop.utils.PasswordUtil;
import com.lifeshop.vo.LoginVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

/**
 * 用户服务
 */
@Service
public class UserService {

    private final UserMapper userMapper;
    private final MerchantMapper merchantMapper;
    private final JwtUtil jwtUtil;

    public UserService(UserMapper userMapper, MerchantMapper merchantMapper, JwtUtil jwtUtil) {
        this.userMapper = userMapper;
        this.merchantMapper = merchantMapper;
        this.jwtUtil = jwtUtil;
    }

    /** 注册 */
    public LoginVO register(RegisterDTO dto) {
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException("该账号已被注册");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(PasswordUtil.encrypt(dto.getPassword()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setGender(0);
        user.setRole("USER");
        user.setStatus(1);
        user.setAvatar("https://picsum.photos/seed/" + dto.getUsername() + "/120/120");
        userMapper.insert(user);
        return new LoginVO(jwtUtil.createToken(user.getId(), user.getUsername(), user.getRole()), user);
    }

    /** 登录 */
    public LoginVO login(LoginDTO dto) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (user == null || !PasswordUtil.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("账号或密码错误");
        }
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException("账号已被禁用，请联系客服");
        }
        LoginVO vo = new LoginVO(jwtUtil.createToken(user.getId(), user.getUsername(), user.getRole()), user);
        if ("MERCHANT".equals(user.getRole())) {
            Merchant merchant = merchantMapper.selectOne(
                    new LambdaQueryWrapper<Merchant>().eq(Merchant::getUserId, user.getId()));
            vo.setMerchant(merchant);
        }
        return vo;
    }

    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    /** 修改资料 */
    public User updateInfo(Long userId, UserUpdateDTO dto) {
        User user = getById(userId);
        if (StringUtils.hasText(dto.getNickname())) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (StringUtils.hasText(dto.getAvatar())) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
        }
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
        return userMapper.selectById(userId);
    }

    /** 修改密码 */
    public void changePassword(Long userId, PasswordDTO dto) {
        User user = getById(userId);
        if (!PasswordUtil.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        if (dto.getOldPassword().equals(dto.getNewPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        user.setPassword(PasswordUtil.encrypt(dto.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /** 后台：用户分页 */
    public PageResult<User> page(String keyword, Integer status, long pageNum, long pageSize) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRole, "USER");
        if (StringUtils.hasText(keyword)) {
            wrapper.and(q -> q.like(User::getUsername, keyword)
                    .or().like(User::getNickname, keyword)
                    .or().like(User::getPhone, keyword));
        }
        if (status != null) {
            wrapper.eq(User::getStatus, status);
        }
        wrapper.orderByDesc(User::getId);
        Page<User> page = userMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page);
    }

    /** 后台：启用 / 禁用 */
    public void updateStatus(Long id, Integer status) {
        User user = getById(id);
        if ("ADMIN".equals(user.getRole())) {
            throw new BusinessException("不能操作管理员账号");
        }
        user.setStatus(status);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(user);
    }

    /** 后台：新增普通用户 */
    public User adminCreateUser(AdminUserCreateDTO dto) {
        Long exists = userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getUsername, dto.getUsername()));
        if (exists != null && exists > 0) {
            throw new BusinessException("该账号已被占用");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(PasswordUtil.encrypt(dto.getPassword()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setPhone(dto.getPhone());
        user.setGender(dto.getGender() == null ? 0 : dto.getGender());
        user.setRole("USER");
        user.setStatus(1);
        user.setAvatar("https://picsum.photos/seed/" + dto.getUsername() + "/120/120");
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userMapper.insert(user);
        return user;
    }
}

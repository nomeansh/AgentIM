package com.AgentIM.business.im.service.impl;

import com.AgentIM.auth.api.model.LoginUser;
import com.AgentIM.auth.api.model.RoleDTO;
import com.AgentIM.business.im.constant.ImConstants;
import com.AgentIM.business.im.domain.bo.ImRegisterBo;
import com.AgentIM.business.im.domain.bo.ImUserAccessPolicyBo;
import com.AgentIM.business.im.domain.bo.ImUserProfileBo;
import com.AgentIM.business.im.domain.entity.ImChat;
import com.AgentIM.business.im.domain.entity.ImChatMember;
import com.AgentIM.business.im.domain.entity.ImUser;
import com.AgentIM.business.im.domain.vo.ImUserAccessPolicyVo;
import com.AgentIM.business.im.domain.vo.ImUserVo;
import com.AgentIM.business.im.enums.ImChatStatus;
import com.AgentIM.business.im.enums.ImChatType;
import com.AgentIM.business.im.enums.ImMemberRole;
import com.AgentIM.business.im.mapper.ImChatMapper;
import com.AgentIM.business.im.mapper.ImChatMemberMapper;
import com.AgentIM.business.im.mapper.ImUserMapper;
import com.AgentIM.business.im.permission.ImPermissionService;
import com.AgentIM.business.im.service.IImUserService;
import com.AgentIM.business.im.support.ImAuditService;
import com.AgentIM.business.im.support.ImPasswordCodec;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.exception.ServiceException;
import org.dromara.common.core.utils.MapstructUtils;
import org.dromara.common.core.utils.StringUtils;
import org.dromara.common.json.utils.JsonUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

/**
 * IM 用户资料服务实现。
 */
@Service
@RequiredArgsConstructor
public class ImUserServiceImpl implements IImUserService {

    private final ImUserMapper userMapper;
    private final ImChatMapper chatMapper;
    private final ImChatMemberMapper chatMemberMapper;
    private final ImPermissionService permissionService;
    private final ImAuditService auditService;

    /**
     * 注册 IM 用户并创建保存的消息会话。
     *
     * <p>方法先校验用户名唯一，再写入用户资料和 saved 聊天。保存的消息会话与用户注册处于同一
     * 数据库事务，保证 P0 “注册后即可使用保存的消息” 的业务闭环。</p>
     *
     * @param bo 注册入参
     * @return 新用户 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long register(ImRegisterBo bo) {
        String username = normalizeUsername(bo.getUsername());
        if (userMapper.selectByUsername(username) != null) {
            throw new ServiceException("用户名已存在");
        }
        ImUser user = new ImUser();
        user.setUsername(username);
        user.setPassword(ImPasswordCodec.encode(bo.getPassword()));
        user.setNickname(username);
        user.setStatus("0");
        user.setDelFlag(ImConstants.DEL_FLAG_NORMAL);
        userMapper.insert(user);
        createSavedChat(user.getId());
        auditService.record(null, "user", user.getId(), "REGISTER_USER", user.getId(),
            "注册 IM 用户 " + username, null);
        return user.getId();
    }

    /**
     * 获取当前登录用户资料。
     *
     * <p>当前用户 ID 来自 Sa-Token 上下文，查询失败会被视为账号状态异常。</p>
     *
     * @return 当前用户资料
     */
    @Override
    public ImUserVo getProfile() {
        return MapstructUtils.convert(requireUser(permissionService.currentUserId()), ImUserVo.class);
    }

    /**
     * 更新当前登录用户资料。
     *
     * <p>方法只更新展示字段，并在更新后重新读取完整视图返回，避免调用方拿到部分字段对象。</p>
     *
     * @param bo 用户资料更新入参
     * @return 更新后的用户资料
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImUserVo updateProfile(ImUserProfileBo bo) {
        Long userId = permissionService.currentUserId();
        ImUser user = requireUser(userId);
        user.setNickname(bo.getNickname());
        user.setAvatar(bo.getAvatar());
        user.setBio(bo.getBio());
        user.setPhone(bo.getPhone());
        user.setEmail(bo.getEmail());
        userMapper.updateById(user);
        auditService.record(null, "user", userId, "UPDATE_PROFILE", userId,
            "更新用户资料", bo);
        return getUser(userId);
    }

    /**
     * 查询用户级数据访问策略。
     *
     * <p>策略字段不放进普通用户公开资料，避免资料卡泄露后续业务资源的授权细节。</p>
     *
     * @param userId 用户 ID
     * @return 用户访问策略
     */
    @Override
    public ImUserAccessPolicyVo getAccessPolicy(Long userId) {
        return buildAccessPolicyVo(requireUser(userId));
    }

    /**
     * 更新用户级数据访问策略。
     *
     * <p>当前 P0 仍以聊天成员关系做即时通讯数据权限；本方法只预留用户主体上的非聊天资源策略。</p>
     *
     * @param userId 用户 ID
     * @param bo 策略更新入参
     * @return 更新后的用户访问策略
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImUserAccessPolicyVo updateAccessPolicy(Long userId, ImUserAccessPolicyBo bo) {
        Long actorId = permissionService.currentUserId();
        ImUser user = requireUser(userId);
        String dataScope = normalizeDataScope(bo.getDataScope());
        String permissionTags = normalizeJsonArray(bo.getPermissionTags(), "权限标签");
        String accessPolicy = normalizeJsonObject(bo.getAccessPolicy(), "访问策略");
        if (userMapper.updateAccessPolicy(userId, dataScope, permissionTags, accessPolicy, actorId) < 1) {
            throw new ServiceException("用户访问策略更新失败");
        }
        auditService.record(null, "user", userId, "UPDATE_ACCESS_POLICY", actorId,
            "更新用户数据访问策略 " + user.getUsername(), bo);
        return getAccessPolicy(userId);
    }

    /**
     * 根据用户 ID 查询公开资料。
     *
     * <p>返回对象不包含密码摘要，适合对聊天成员和联系人展示。</p>
     *
     * @param userId 用户 ID
     * @return 用户公开资料
     */
    @Override
    public ImUserVo getUser(Long userId) {
        return MapstructUtils.convert(requireUser(userId), ImUserVo.class);
    }

    /**
     * 搜索用户。
     *
     * <p>空关键词直接返回空列表，避免无条件枚举用户表。limit 会被收敛到 1 到 50。</p>
     *
     * @param keyword 搜索关键词
     * @param limit 最大返回条数
     * @return 用户资料列表
     */
    @Override
    public List<ImUserVo> searchUsers(String keyword, int limit) {
        if (StringUtils.isBlank(keyword)) {
            return List.of();
        }
        return userMapper.searchUsers(keyword.trim(), normalizeLimit(limit, 20));
    }

    /**
     * 根据用户名加载认证用户。
     *
     * <p>该方法为 Auth 服务提供登录用户模型，补齐权限集合、角色集合、租户上下文和用户类型。</p>
     *
     * @param username 用户名
     * @return 登录用户模型
     */
    @Override
    public LoginUser loadLoginUser(String username) {
        ImUser user = userMapper.selectByUsername(normalizeUsername(username));
        if (user == null || !"0".equals(user.getStatus())) {
            throw new ServiceException("用户名或密码错误");
        }
        LoginUser loginUser = new LoginUser();
        loginUser.setTenantId(ImConstants.DEFAULT_TENANT_ID);
        loginUser.setUserId(user.getId());
        loginUser.setUsername(user.getUsername());
        loginUser.setNickname(user.getNickname());
        loginUser.setPassword(user.getPassword());
        loginUser.setUserType(ImConstants.DEFAULT_USER_TYPE);
        loginUser.setMenuPermission(ImConstants.P0_PERMISSIONS);
        loginUser.setRolePermission(ImConstants.P0_ROLES);
        RoleDTO role = new RoleDTO();
        role.setRoleId(1L);
        role.setRoleName("IM 用户");
        role.setRoleKey("im_user");
        loginUser.setRoles(List.of(role));
        return loginUser;
    }

    /**
     * 校验明文密码和存储密码摘要是否匹配。
     *
     * <p>具体摘要格式由 ImPasswordCodec 管理，便于未来升级密码算法时保留兼容逻辑。</p>
     *
     * @param rawPassword 明文密码
     * @param encodedPassword 存储的密码摘要
     * @return true 表示匹配
     */
    @Override
    public boolean passwordMatches(String rawPassword, String encodedPassword) {
        return ImPasswordCodec.matches(rawPassword, encodedPassword);
    }

    /**
     * 根据 ID 获取用户实体并要求存在。
     *
     * <p>该方法同时校验用户状态，停用用户不能参与新的聊天和消息操作。</p>
     *
     * @param userId 用户 ID
     * @return 用户实体
     */
    @Override
    public ImUser requireUser(Long userId) {
        ImUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new ServiceException("用户不存在");
        }
        if (!"0".equals(user.getStatus())) {
            throw new ServiceException("用户已停用");
        }
        return user;
    }

    /**
     * 创建保存的消息聊天。
     *
     * <p>保存的消息是系统自动创建的一人聊天，owner 与成员都是用户本人。该方法只在注册事务中
     * 使用，不暴露给普通接口重复创建。</p>
     *
     * @param userId 用户 ID
     */
    private void createSavedChat(Long userId) {
        ImChat chat = new ImChat();
        chat.setType(ImChatType.SAVED.getCode());
        chat.setTitle("保存的消息");
        chat.setOwnerId(userId);
        chat.setSeq(0L);
        chat.setStatus(ImChatStatus.ACTIVE.getCode());
        chat.setDelFlag(ImConstants.DEL_FLAG_NORMAL);
        chatMapper.insert(chat);

        ImChatMember member = new ImChatMember();
        member.setChatId(chat.getId());
        member.setUserId(userId);
        member.setRole(ImMemberRole.OWNER.getCode());
        member.setJoinedTime(new Date());
        member.setMuted("0");
        member.setDelFlag(ImConstants.DEL_FLAG_NORMAL);
        chatMemberMapper.insert(member);
    }

    /**
     * 构造用户级访问策略视图。
     *
     * @param user 用户实体
     * @return 访问策略视图
     */
    private ImUserAccessPolicyVo buildAccessPolicyVo(ImUser user) {
        ImUserAccessPolicyVo vo = new ImUserAccessPolicyVo();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setStatus(user.getStatus());
        vo.setDataScope(user.getDataScope());
        vo.setPermissionTags(user.getPermissionTags());
        vo.setAccessPolicy(user.getAccessPolicy());
        vo.setUpdateTime(user.getUpdateTime());
        return vo;
    }

    /**
     * 标准化用户级数据访问范围。
     *
     * @param dataScope 数据访问范围
     * @return 规范值
     */
    private String normalizeDataScope(String dataScope) {
        String normalized = StringUtils.trim(dataScope);
        if ("standard".equals(normalized)
            || "restricted".equals(normalized)
            || "trusted".equals(normalized)
            || "blocked".equals(normalized)) {
            return normalized;
        }
        throw new ServiceException("数据访问范围只能是 standard、restricted、trusted、blocked");
    }

    /**
     * 校验并标准化 JSON 数组字符串。
     *
     * @param value JSON 字符串
     * @param fieldName 字段名称
     * @return 规范值
     */
    private String normalizeJsonArray(String value, String fieldName) {
        String normalized = StringUtils.trim(value);
        if (!JsonUtils.isJsonArray(normalized)) {
            throw new ServiceException(fieldName + "必须是 JSON 数组");
        }
        return normalized;
    }

    /**
     * 校验并标准化 JSON 对象字符串。
     *
     * @param value JSON 字符串
     * @param fieldName 字段名称
     * @return 规范值
     */
    private String normalizeJsonObject(String value, String fieldName) {
        String normalized = StringUtils.trim(value);
        if (!JsonUtils.isJsonObject(normalized)) {
            throw new ServiceException(fieldName + "必须是 JSON 对象");
        }
        return normalized;
    }

    /**
     * 标准化用户名。
     *
     * <p>用户名统一去除首尾空白并转小写，保证 @handle 唯一性不受大小写差异影响。</p>
     *
     * @param username 原始用户名
     * @return 标准化用户名
     */
    private String normalizeUsername(String username) {
        if (StringUtils.isBlank(username)) {
            throw new ServiceException("用户名不能为空");
        }
        return username.trim().toLowerCase();
    }

    /**
     * 收敛查询条数。
     *
     * <p>对外搜索接口的 limit 需要限制范围，防止客户端传入过大值造成数据库压力。</p>
     *
     * @param limit 调用方传入条数
     * @param defaultValue 默认条数
     * @return 1 到 50 范围内的条数
     */
    private int normalizeLimit(int limit, int defaultValue) {
        if (limit <= 0) {
            return defaultValue;
        }
        return Math.min(limit, 50);
    }
}

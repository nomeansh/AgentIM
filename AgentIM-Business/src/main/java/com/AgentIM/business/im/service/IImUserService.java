package com.AgentIM.business.im.service;

import com.AgentIM.business.im.domain.bo.ImRegisterBo;
import com.AgentIM.business.im.domain.bo.ImUserAccessPolicyBo;
import com.AgentIM.business.im.domain.bo.ImUserProfileBo;
import com.AgentIM.business.im.domain.entity.ImUser;
import com.AgentIM.business.im.domain.vo.ImUserAccessPolicyVo;
import com.AgentIM.business.im.domain.vo.ImUserVo;
import com.AgentIM.auth.api.model.LoginUser;

import java.util.List;

/**
 * IM 用户资料服务。
 *
 * <p>该服务负责 P0 用户资料、登录账号创建、认证用户装配和保存的消息初始化。它不处理 Sa-Token
 * 会话创建，认证会话仍由 Auth 服务完成。</p>
 */
public interface IImUserService {

    /**
     * 注册 IM 用户并创建保存的消息会话。
     *
     * <p>注册过程在同一事务内写入 im_user、im_chat 和 im_chat_member，保证用户一旦注册成功就
     * 可以使用保存的消息作为云端便签。</p>
     *
     * @param bo 注册入参
     * @return 新用户 ID
     */
    Long register(ImRegisterBo bo);

    /**
     * 获取当前登录用户资料。
     *
     * <p>方法从 Sa-Token 登录上下文读取 userId，再查询 IM 用户资料。若资料不存在，将抛出业务
     * 异常提示账号状态异常。</p>
     *
     * @return 当前用户资料
     */
    ImUserVo getProfile();

    /**
     * 更新当前登录用户资料。
     *
     * <p>仅更新昵称、头像、简介、电话和邮箱等展示字段，不修改密码、状态、权限和登录上下文。</p>
     *
     * @param bo 用户资料更新入参
     * @return 更新后的用户资料
     */
    ImUserVo updateProfile(ImUserProfileBo bo);

    /**
     * 查询用户级数据访问策略。
     *
     * <p>该方法用于未来管理员或运维角色查看用户主体上的非聊天数据权限预留字段。</p>
     *
     * @param userId 用户 ID
     * @return 用户访问策略
     */
    ImUserAccessPolicyVo getAccessPolicy(Long userId);

    /**
     * 更新用户级数据访问策略。
     *
     * <p>该策略只作为非聊天资源的扩展预留，不改变 im_chat_member 已有聊天级权限模型。</p>
     *
     * @param userId 用户 ID
     * @param bo 策略更新入参
     * @return 更新后的用户访问策略
     */
    ImUserAccessPolicyVo updateAccessPolicy(Long userId, ImUserAccessPolicyBo bo);

    /**
     * 根据用户 ID 查询公开资料。
     *
     * <p>用于联系人、成员列表和资料卡片展示。查询结果不包含密码摘要等敏感字段。</p>
     *
     * @param userId 用户 ID
     * @return 用户公开资料
     */
    ImUserVo getUser(Long userId);

    /**
     * 搜索用户。
     *
     * <p>P0 使用数据库 ILIKE 搜索用户名和昵称，并限制返回数量，避免一次搜索返回过多用户资料。</p>
     *
     * @param keyword 搜索关键词
     * @param limit 最大返回条数
     * @return 用户资料列表
     */
    List<ImUserVo> searchUsers(String keyword, int limit);

    /**
     * 根据用户名加载认证用户。
     *
     * <p>Auth 服务通过 Dubbo 调用该方法完成密码登录前的账号解析。返回对象会带上 P0 IM 权限集，
     * 供 Sa-Token 注解校验使用。</p>
     *
     * @param username 用户名
     * @return 登录用户模型
     */
    LoginUser loadLoginUser(String username);

    /**
     * 校验明文密码和存储密码摘要是否匹配。
     *
     * <p>密码校验逻辑放在 Business 侧，Auth 服务不需要理解密码摘要格式，从而保持认证入口与
     * 账号存储解耦。</p>
     *
     * @param rawPassword 明文密码
     * @param encodedPassword 存储的密码摘要
     * @return true 表示匹配，false 表示不匹配
     */
    boolean passwordMatches(String rawPassword, String encodedPassword);

    /**
     * 根据 ID 获取用户实体并要求存在。
     *
     * <p>业务服务内部用于成员、联系人、消息发送者等场景的强校验。若用户不存在或已删除，会抛出
     * 业务异常。</p>
     *
     * @param userId 用户 ID
     * @return 用户实体
     */
    ImUser requireUser(Long userId);
}

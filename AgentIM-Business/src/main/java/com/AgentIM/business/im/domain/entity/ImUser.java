package com.AgentIM.business.im.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.dromara.common.encrypt.annotation.EncryptField;
import org.dromara.common.mybatis.core.domain.BaseEntity;

/**
 * IM 用户资料实体。
 *
 * <p>该实体复用用户 ID 作为全局身份标识，保存 @handle、昵称、头像和登录密码摘要等 IM
 * 专属字段。P0 认证中心通过 Business 模块的 Dubbo 契约读取该表完成登录。</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("im_user")
public class ImUser extends BaseEntity {

    /**
     * 用户 ID，框架雪花 ID。
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 用户名 @handle，全局唯一。
     */
    private String username;

    /**
     * 登录密码哈希，当前新数据使用 BCrypt，兼容早期 P0 开发期 SHA-256 摘要。
     */
    private String password;

    /**
     * 展示昵称。
     */
    private String nickname;

    /**
     * 头像 URL 或资源标识。
     */
    private String avatar;

    /**
     * 个人简介。
     */
    private String bio;

    /**
     * 手机号，开启 common-encrypt 后通过 MyBatis 字段加解密保存。
     */
    @EncryptField
    private String phone;

    /**
     * 邮箱，开启 common-encrypt 后通过 MyBatis 字段加解密保存。
     */
    @EncryptField
    private String email;

    /**
     * 用户状态，0 表示正常，1 表示停用。
     */
    private String status;

    /**
     * 用户级数据访问范围预留：standard、restricted、trusted、blocked。
     */
    private String dataScope;

    /**
     * 用户权限标签 JSONB 数组，Java 侧以 JSON 字符串保存。
     */
    private String permissionTags;

    /**
     * 用户级数据访问策略 JSONB 对象，Java 侧以 JSON 字符串保存。
     */
    private String accessPolicy;

    /**
     * 逻辑删除标记，0 表示正常，2 表示删除。
     */
    @TableLogic(value = "0", delval = "2")
    private String delFlag;
}

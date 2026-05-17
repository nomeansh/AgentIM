package com.AgentIM.business.im.mapper;

import com.AgentIM.business.im.domain.entity.ImUser;
import com.AgentIM.business.im.domain.vo.ImUserVo;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.dromara.common.mybatis.core.mapper.BaseMapperPlus;

import java.util.List;

/**
 * IM 用户数据访问层。
 *
 * <p>除 MyBatis-Plus 提供的基础 CRUD 外，该 Mapper 负责用户名登录解析和用户搜索。</p>
 */
public interface ImUserMapper extends BaseMapperPlus<ImUser, ImUserVo> {

    /**
     * 根据用户名查询未删除用户。
     *
     * <p>认证中心登录、注册唯一性校验和 @handle 精确查找都使用该方法。查询固定过滤 del_flag，
     * 确保已注销或逻辑删除用户不能继续登录。</p>
     *
     * @param username 用户名 @handle
     * @return 匹配的用户实体，不存在时返回 null
     */
    @Select("""
        SELECT * FROM im_user
        WHERE username = #{username}
          AND del_flag = '0'
        LIMIT 1
        """)
    ImUser selectByUsername(@Param("username") String username);

    /**
     * 根据用户名或昵称进行模糊搜索。
     *
     * <p>P0 用户搜索明确保留 PostgreSQL ILIKE 轻量实现，当前阶段不接入 Elasticsearch。
     * 该查询只返回正常用户，避免搜索结果泄露已删除账号。</p>
     *
     * @param keyword 搜索关键词
     * @param limit 最大返回条数
     * @return 用户资料视图列表
     */
    @Select("""
        SELECT id, username, nickname, avatar, bio, phone, email, status, create_time
        FROM im_user
        WHERE del_flag = '0'
          AND status = '0'
          AND (username ILIKE CONCAT('%', #{keyword}, '%')
               OR nickname ILIKE CONCAT('%', #{keyword}, '%'))
        ORDER BY username ASC
        LIMIT #{limit}
        """)
    List<ImUserVo> searchUsers(@Param("keyword") String keyword, @Param("limit") int limit);

    /**
     * 更新用户级数据访问策略预留字段。
     *
     * <p>JSONB 字段显式 cast，避免 PostgreSQL 在 JDBC 预编译参数为字符串时拒绝隐式类型转换。</p>
     *
     * @param userId 用户 ID
     * @param dataScope 用户级数据访问范围
     * @param permissionTags 用户权限标签 JSON 数组
     * @param accessPolicy 用户级访问策略 JSON 对象
     * @param actorId 操作人 ID
     * @return 更新行数
     */
    @Update("""
        UPDATE im_user
        SET data_scope = #{dataScope},
            permission_tags = CAST(#{permissionTags} AS jsonb),
            access_policy = CAST(#{accessPolicy} AS jsonb),
            update_by = #{actorId},
            update_time = CURRENT_TIMESTAMP
        WHERE id = #{userId}
          AND del_flag = '0'
        """)
    int updateAccessPolicy(@Param("userId") Long userId,
                           @Param("dataScope") String dataScope,
                           @Param("permissionTags") String permissionTags,
                           @Param("accessPolicy") String accessPolicy,
                           @Param("actorId") Long actorId);

    /**
     * 执行 IM 用户表联通检查。
     *
     * <p>该查询只依赖 `im_user` 表存在并可访问，不要求表内已有用户数据。联调检查服务使用它验证
     * Business 服务连接的数据库已经执行 P0 建表脚本，且当前数据源具备读取 IM 核心表的权限。</p>
     *
     * @return 固定返回 1；如果表不存在或数据库不可用会由 MyBatis 抛出异常
     */
    @Select("SELECT 1 FROM im_user LIMIT 1")
    Integer checkImUserTable();
}

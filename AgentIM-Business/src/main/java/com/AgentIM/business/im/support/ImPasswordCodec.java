package com.AgentIM.business.im.support;

import cn.hutool.crypto.digest.BCrypt;
import org.dromara.common.core.exception.ServiceException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * IM 密码摘要工具。
 *
 * <p>密码存储使用 Hutool BCrypt 生成不可逆哈希，并通过格式前缀记录算法版本。Business 模块
 * 已依赖 common-encrypt 公共加解密模块，具体安全算法来自公共依赖体系，而不是在业务代码里
 * 手写加密实现。该类同时兼容早期 P0 开发期的 {@code sha256$} 摘要，便于本地数据平滑迁移。</p>
 */
public final class ImPasswordCodec {

    private static final String BCRYPT_PREFIX = "bcrypt$";

    private static final String LEGACY_SHA256_PREFIX = "sha256$";

    /**
     * 阻止实例化密码工具类。
     *
     * <p>密码摘要是无状态纯函数，保持私有构造器可以避免被误注入或持有状态。</p>
     */
    private ImPasswordCodec() {
    }

    /**
     * 将明文密码转换为存储摘要。
     *
     * <p>方法返回带格式前缀的 BCrypt 哈希。BCrypt 会为每次编码生成独立盐值，因此同一个明文密码
     * 多次注册也不会得到相同存储值。明文密码不会在该方法外被保存，调用者应只把返回值写入数据库。</p>
     *
     * @param rawPassword 明文密码
     * @return 带格式前缀的密码摘要
     */
    public static String encode(String rawPassword) {
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new ServiceException("密码不能为空");
        }
        return BCRYPT_PREFIX + BCrypt.hashpw(rawPassword);
    }

    /**
     * 校验明文密码是否匹配存储摘要。
     *
     * <p>方法优先识别当前 {@code bcrypt$} 格式，并兼容早期 {@code sha256$} 摘要和开发期可能
     * 存在的明文种子数据。兼容逻辑只用于存量数据读取，新注册用户一律写入 BCrypt 格式。</p>
     *
     * @param rawPassword 明文密码
     * @param encodedPassword 数据库存储的密码值
     * @return true 表示密码匹配
     */
    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        if (encodedPassword.startsWith(BCRYPT_PREFIX)) {
            return BCrypt.checkpw(rawPassword, encodedPassword.substring(BCRYPT_PREFIX.length()));
        }
        if (encodedPassword.startsWith(LEGACY_SHA256_PREFIX)) {
            byte[] expected = encodedPassword.substring(LEGACY_SHA256_PREFIX.length()).getBytes(StandardCharsets.UTF_8);
            byte[] actual = digest(rawPassword).getBytes(StandardCharsets.UTF_8);
            return MessageDigest.isEqual(expected, actual);
        }
        return MessageDigest.isEqual(
            rawPassword.getBytes(StandardCharsets.UTF_8),
            encodedPassword.getBytes(StandardCharsets.UTF_8)
        );
    }

    /**
     * 计算 SHA-256 十六进制摘要。
     *
     * <p>该方法只负责不可逆摘要计算，不添加业务前缀。调用者通过 encode 方法获得最终存储格式。</p>
     *
     * @param rawPassword 明文密码
     * @return 十六进制摘要字符串
     */
    private static String digest(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new ServiceException("当前 JDK 不支持 SHA-256 摘要算法");
        }
    }
}

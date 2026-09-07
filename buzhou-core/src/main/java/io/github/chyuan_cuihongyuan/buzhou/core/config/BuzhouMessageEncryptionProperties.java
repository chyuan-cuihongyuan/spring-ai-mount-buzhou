package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 消息静态加密装配属性（spec 333 / T658，前缀
 * {@code buzhou.security.message-encryption}）。未配 master-key = 不装配
 * 包装器（零行为变化）。
 *
 * @param masterKey         当前主钥（Base64，16/24/32 字节 AES 钥——如 openssl rand -base64 32）
 * @param previousMasterKey 前代主钥（轮换窗口——仅解密；新写永远用 master-key）
 */
@ConfigurationProperties(prefix = "buzhou.security.message-encryption")
public record BuzhouMessageEncryptionProperties(
        String masterKey,
        String previousMasterKey) {
}

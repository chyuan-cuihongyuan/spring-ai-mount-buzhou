package io.github.chyuan_cuihongyuan.buzhou.dashboard.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 可视化后台装配属性（spec 03 / 09，前缀 {@code buzhou.observe.dashboard}；impl-48 安全收口）。
 *
 * <p>安全模型对齐 Spring Boot Actuator：默认只绑 loopback；绑定非 loopback 且未设
 * {@code auth-token} 时<b>启动失败</b>（暴露面与鉴权强制配对），设了 token 则启动 WARN
 * 提示生产建议走 OTel。
 *
 * @param enabled    总开关（默认 false，开发调试时开启）
 * @param port       独立端口；{@code 0} = 随机端口（默认）
 * @param pathPrefix 静态资源与 API 前缀（默认 {@code /buzhou}；不得与 /api 冲突）
 * @param bindAddress 绑定地址（默认 127.0.0.1；0.0.0.0 等非 loopback 必须配 auth-token）
 * @param authToken  Bearer 鉴权 token（设置后全部 API 与静态页要求 Authorization 头；
 *                   值支持 {@code ${ENV:}} 占位经 Spring 解析）
 */
@ConfigurationProperties(prefix = "buzhou.observe.dashboard")
public record DashboardProperties(Boolean enabled, Integer port, String pathPrefix,
                                   String bindAddress, String authToken) {

    /** 多构造器场景下显式指定绑定构造器（3 参兼容构造仅供编程式使用）。 */
    @org.springframework.boot.context.properties.bind.ConstructorBinding
    public DashboardProperties {
        enabled = enabled == null ? false : enabled;
        port = port == null ? 0 : port;
        pathPrefix = (pathPrefix == null || pathPrefix.isBlank()) ? "/buzhou" : pathPrefix.trim();
        bindAddress = (bindAddress == null || bindAddress.isBlank()) ? "127.0.0.1" : bindAddress.trim();
        authToken = (authToken == null || authToken.isBlank()) ? null : authToken;
        if (port < 0 || port > 65535) {
            throw new BuzhouConfigurationException(
                    "buzhou.observe.dashboard.port（" + port + "）越界", "取 0（随机）或 1-65535");
        }
        try {
            InetAddress.getByName(bindAddress);
        } catch (UnknownHostException e) {
            throw new BuzhouConfigurationException(
                    "buzhou.observe.dashboard.bind-address（" + bindAddress + "）无法解析",
                    "使用 IP 字面量（如 127.0.0.1 / 0.0.0.0）或可解析主机名");
        }
    }

    /** 既有 3 参构造兼容。 */
    public DashboardProperties(Boolean enabled, Integer port, String pathPrefix) {
        this(enabled, port, pathPrefix, null, null);
    }
}

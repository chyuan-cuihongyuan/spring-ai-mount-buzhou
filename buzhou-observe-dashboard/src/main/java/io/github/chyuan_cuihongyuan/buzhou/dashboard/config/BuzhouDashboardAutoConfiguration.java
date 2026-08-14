package io.github.chyuan_cuihongyuan.buzhou.dashboard.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.dashboard.DashboardModule;
import io.github.chyuan_cuihongyuan.buzhou.dashboard.SkillAdminPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.lang.System.Logger;
import java.lang.System.Logger.Level;
import java.net.InetAddress;

/**
 * 可视化后台自装配（spec 03 / 09；impl-48 / spec 14 §D 安全收口）。
 *
 * <p><b>缺省关闭</b>（{@code buzhou.observe.dashboard.enabled} 默认 false）。显式开启后构建并启动
 * {@link DashboardModule} 内嵌 Web 服务（默认只绑 127.0.0.1，随机端口）。安全模型对齐
 * Spring Boot Actuator「暴露面与鉴权强制配对」：
 * <ul>
 *   <li>绑定非 loopback 且未设 {@code auth-token} → <b>启动失败</b>
 *       （BuzhouConfigurationException，经 FailureAnalyzer 翻译修法）；</li>
 *   <li>绑定非 loopback 且已设 token → 启动 WARN（生产监控建议走 OTel 导出）；</li>
 *   <li>设 token 后全部 API 与静态页要求 {@code Authorization: Bearer} 头。</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "buzhou.observe.dashboard", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(DashboardProperties.class)
public class BuzhouDashboardAutoConfiguration {

    private static final Logger LOGGER = System.getLogger(BuzhouDashboardAutoConfiguration.class.getName());

    @Bean(destroyMethod = "close")
    public DashboardModule buzhouDashboardModule(BuzhouStores stores, DashboardProperties props,
                                                 ObjectProvider<SkillAdminPort> skillAdmin) {
        boolean loopback = isLoopback(props.bindAddress());
        if (!loopback && props.authToken() == null) {
            // impl-48 fail-fast：公网/内网暴露面零鉴权 = Skill 正文（模型行为）可被任意客户端篡改
            throw new BuzhouConfigurationException(
                    "buzhou.observe.dashboard.bind-address=" + props.bindAddress()
                            + "（非 loopback）但未设置 auth-token",
                    "设置 buzhou.observe.dashboard.auth-token（支持 ${ENV:} 占位），"
                            + "或改回 bind-address=127.0.0.1 仅本机访问");
        }
        if (!loopback) {
            LOGGER.log(Level.WARNING,
                    "buzhou dashboard 绑定非 loopback 地址（" + props.bindAddress() + "）："
                            + "已启用 Bearer 鉴权；生产环境长期监控建议改用 OTel 导出（buzhou-observe-otel）");
        }
        return DashboardModule.builder(stores.observabilityStore())
                .skillAdmin(skillAdmin.getIfAvailable())
                .port(props.port())
                .pathPrefix(props.pathPrefix())
                .bindAddress(props.bindAddress())
                .authToken(props.authToken())
                .build()
                .start();
    }

    private static boolean isLoopback(String bindAddress) {
        try {
            return InetAddress.getByName(bindAddress).isLoopbackAddress();
        } catch (java.net.UnknownHostException e) {
            return false; // 不可解析地址按非 loopback 对待（属性绑定层已 fail-fast，此处防御）
        }
    }
}

package io.github.chyuan_cuihongyuan.buzhou.dashboard.config;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.dashboard.DashboardModule;
import io.github.chyuan_cuihongyuan.buzhou.dashboard.SkillAdminPort;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * 可视化后台自装配（spec 03 / 09 / ticket 22）。
 *
 * <p><b>缺省关闭</b>（{@code buzhou.observe.dashboard.enabled} 默认 false）。显式开启后构建并启动
 * {@link DashboardModule} 内嵌 Web 服务（独立端口，默认随机），暴露会话回放 / 注入快照 / token 耗时查询 API。
 * 销毁时停止服务。查询侧复用 {@link BuzhouStores} 的 {@code observabilityStore}；Skill 管理页经
 * {@link SkillAdminPort} SPI 适配注入（缺失则该端点 501）。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "buzhou.observe.dashboard", name = "enabled", havingValue = "true")
@EnableConfigurationProperties(DashboardProperties.class)
public class BuzhouDashboardAutoConfiguration {

    @Bean(destroyMethod = "close")
    public DashboardModule buzhouDashboardModule(BuzhouStores stores, DashboardProperties props,
                                                 ObjectProvider<SkillAdminPort> skillAdmin) {
        return DashboardModule.builder(stores.observabilityStore())
                .skillAdmin(skillAdmin.getIfAvailable())
                .port(props.port())
                .pathPrefix(props.pathPrefix())
                .build()
                .start();
    }
}

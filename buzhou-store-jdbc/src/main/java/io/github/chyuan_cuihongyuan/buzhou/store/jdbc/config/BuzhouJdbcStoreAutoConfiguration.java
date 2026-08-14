package io.github.chyuan_cuihongyuan.buzhou.store.jdbc.config;

import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunRegistry;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLog;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.store.jdbc.Dialect;
import io.github.chyuan_cuihongyuan.buzhou.store.jdbc.JdbcBuzhouRecoveryStores;
import io.github.chyuan_cuihongyuan.buzhou.store.jdbc.JdbcBuzhouStores;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * JDBC store 自装配（spec 08 / 09 / ticket 22 + spec 13 §stores-5 / ticket 31 + ticket 32）。
 *
 * <p>当 {@code buzhou.store.type=jdbc} 时由 {@link JdbcBuzhouStores#createWithRecovery} 用容器内
 * {@link DataSource} 建库（经 {@code SchemaMigrator} 版本化迁移，按
 * {@code buzhou.store.jdbc.dialect} 选方言脚本目录）产出 {@link JdbcBuzhouRecoveryStores}，
 * 并从中派生：
 * <ul>
 *   <li>{@link BuzhouStores}：core 六槽视图（替换内存默认，供内核 runtime 消费）；</li>
 *   <li>恢复设施 {@link RunRegistry} / {@link ToolCallLog}：受
 *       {@code buzhou.store.jdbc.recovery-enabled}（默认开）开关控制，供
 *       {@code RecoverySupport.attach(...)} 挂接 proactive 恢复。</li>
 * </ul>
 * 写失败策略经 {@code buzhou.store.write-failure-policy}（ticket 32，默认 FAIL_TURN）
 * 传入观测槽装饰。需要容器内已有 {@link DataSource} bean
 * （由 Spring Boot 的 DataSource 自动装配或业务侧提供）。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "buzhou.store", name = "type", havingValue = "jdbc")
@EnableConfigurationProperties({JdbcStoreProperties.class, WriteFailurePolicyProperties.class})
public class BuzhouJdbcStoreAutoConfiguration {

    /** 完整组合（6 槽 + 恢复设施）：store.type=jdbc 时总是装配，schema 迁移随之执行。 */
    @Bean
    @ConditionalOnMissingBean
    public JdbcBuzhouRecoveryStores jdbcBuzhouRecoveryStores(
            DataSource dataSource, JdbcStoreProperties props, WriteFailurePolicyProperties writePolicy) {
        // impl-42 / spec 13 §T68：dialect=AUTO（缺省）按连接元数据探测；拼错值 fail-fast 带指引
        Dialect dialect;
        if ("AUTO".equals(props.dialect())) {
            dialect = Dialect.detect(dataSource);
        } else {
            try {
                dialect = Dialect.valueOf(props.dialect());
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("buzhou.store.jdbc.dialect=["
                        + props.dialect() + "] 不是有效方言（H2 / MYSQL / POSTGRESQL / AUTO=自动探测）", e);
            }
        }
        return JdbcBuzhouStores.createWithRecovery(dataSource, dialect,
                writePolicy.writeFailurePolicy());
    }

    /** core 六槽视图（由完整组合派生，保持既有 bean 形状兼容）。 */
    @Bean
    @ConditionalOnMissingBean
    public BuzhouStores buzhouStores(JdbcBuzhouRecoveryStores recoveryStores) {
        return recoveryStores.stores();
    }

    /** Run 注册表（恢复设施，随 recovery-enabled 开关装配）。 */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "buzhou.store.jdbc", name = "recovery-enabled",
            havingValue = "true", matchIfMissing = true)
    public RunRegistry buzhouRunRegistry(JdbcBuzhouRecoveryStores recoveryStores) {
        return recoveryStores.runRegistry();
    }

    /** 事件溯源工具调用日志（恢复设施，随 recovery-enabled 开关装配）。 */
    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "buzhou.store.jdbc", name = "recovery-enabled",
            havingValue = "true", matchIfMissing = true)
    public ToolCallLog buzhouToolCallLog(JdbcBuzhouRecoveryStores recoveryStores) {
        return recoveryStores.toolCallLog();
    }
}

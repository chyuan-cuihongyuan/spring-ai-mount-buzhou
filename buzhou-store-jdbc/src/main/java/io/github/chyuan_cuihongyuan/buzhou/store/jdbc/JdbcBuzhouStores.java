package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;

/**
 * JDBC store 组合工厂（spec 08/09 ticket 22 → spec 13 §stores-5 ticket 31 → ticket 32 事务接线）。
 *
 * <p>建库一律经 {@link SchemaMigrator} 版本化迁移（幂等、可升级、多实例冷启动安全）；
 * 建库后组装 core 六槽 {@link BuzhouStores} 与恢复设施
 * （{@link JdbcRunRegistry} / {@link JdbcToolCallLog}）。
 *
 * <p><b>事务接线（ticket 32 / spec 13 §stores-7）</b>：共享同一 {@link TransactionTemplate}
 * （即 {@link JdbcUnitOfWork} 的事务源）注入各多语句写 store——调用方显式开 UoW 时写进其
 * 事务，未开时各写自开短事务（先删后插不再留崩溃半份窗口）；摘要版本生成经
 * FOR UPDATE + 撞唯一索引重试原子化；观测槽按
 * {@code buzhou.store.write-failure-policy} 经 {@link DegradingObservabilityStore} 装饰。
 */
public final class JdbcBuzhouStores {

    private JdbcBuzhouStores() {
    }

    /**
     * 全量设施工厂（推荐入口，ticket 31）：先做版本化迁移，再返回含恢复设施的完整组合。
     * 写失败策略为默认 {@link WriteFailurePolicy#FAIL_TURN}。
     *
     * @param dataSource 目标数据源
     * @param dialect    SQL 方言
     * @return 6 槽核心存储 + RunRegistry + ToolCallLog 的完整组合
     */
    public static JdbcBuzhouRecoveryStores createWithRecovery(DataSource dataSource, Dialect dialect) {
        return createWithRecovery(dataSource, dialect, WriteFailurePolicy.FAIL_TURN);
    }

    /**
     * 全量设施工厂 + 写失败策略（ticket 32）：观测槽按策略装饰
     * （DEGRADE = 观测类写 WARN + 计数继续；FAIL_TURN = 原样抛）。
     *
     * @param dataSource          目标数据源
     * @param dialect             SQL 方言
     * @param writeFailurePolicy  写失败策略（null 按 FAIL_TURN）
     * @return 6 槽核心存储 + RunRegistry + ToolCallLog 的完整组合
     */
    public static JdbcBuzhouRecoveryStores createWithRecovery(
            DataSource dataSource, Dialect dialect, WriteFailurePolicy writeFailurePolicy) {
        SchemaMigrator.migrate(dataSource, dialect);
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        TransactionTemplate tx = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
        ObservabilityStore observability = new JdbcObservabilityStore(jdbc);
        if (writeFailurePolicy == WriteFailurePolicy.DEGRADE) {
            // DEGRADE 才装饰（观测类写 WARN + 计数继续）；FAIL_TURN 保持既有原样抛语义
            observability = new DegradingObservabilityStore(observability, writeFailurePolicy);
        }
        return new JdbcBuzhouRecoveryStores(
                new BuzhouStores(
                        new JdbcMessageStore(jdbc),
                        new JdbcSummaryStore(jdbc, dialect, tx),
                        new JdbcSessionStateStore(jdbc, tx),
                        new JdbcSessionLeaseStore(jdbc),
                        observability,
                        new JdbcUnitOfWork(tx)),
                new JdbcRunRegistry(jdbc, tx),
                new JdbcToolCallLog(jdbc, tx));
    }

    /**
     * 建 6 槽核心存储组合。
     *
     * @param dataSource 目标数据源
     * @param dialect    SQL 方言
     * @return core 六槽存储组合
     * @deprecated 改用 {@link #createWithRecovery}（含恢复设施，且经版本化迁移建库）。
     *     保留以兼容既有调用方，等价于 {@code createWithRecovery(dataSource, dialect).stores()}；
     *     建库路径已切换到 {@link SchemaMigrator}（修复 MySQL 第二次启动因索引重名必失败的问题）。
     */
    @Deprecated(since = "0.1.0", forRemoval = false)
    public static BuzhouStores create(DataSource dataSource, Dialect dialect) {
        return createWithRecovery(dataSource, dialect).stores();
    }
}

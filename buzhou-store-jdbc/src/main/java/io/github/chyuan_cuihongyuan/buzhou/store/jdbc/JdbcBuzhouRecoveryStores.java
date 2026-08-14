package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunRegistry;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLog;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionLeaseStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SummaryStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.UnitOfWork;

/**
 * JDBC 全量设施组合（ticket 31 / spec 13 §stores-5）：既有 6 槽 {@link BuzhouStores}
 * 之上叠加恢复设施（{@link RunRegistry} + {@link ToolCallLog}）。
 *
 * <p><b>演进策略</b>：core 的 {@link BuzhouStores} 是 record，直接加槽会破坏其二进制兼容，
 * 故恢复设施以本模块内的新组合形状承载（不再改 core）；旧工厂
 * {@link JdbcBuzhouStores#create} 保留 deprecated 并委托到
 * {@link JdbcBuzhouStores#createWithRecovery}。
 *
 * <p><b>用法</b>：恢复三件套经 core 的 {@code RecoverySupport.attach(config,
 * full.runRegistry(), full.toolCallLog(), appId)} 挂进 RuntimeConfig。
 *
 * @param stores      6 槽核心存储组合（消息/摘要/状态/租约/观测/工作单元）
 * @param runRegistry Run 注册表（proactive 恢复：重启后枚举在途 run）
 * @param toolCallLog 事件溯源工具调用日志（exactly-once 回放证据层）
 */
public record JdbcBuzhouRecoveryStores(
        BuzhouStores stores,
        RunRegistry runRegistry,
        ToolCallLog toolCallLog) {

    /** 消息存储（委托 {@link #stores()}）。 */
    public MessageStore messageStore() {
        return stores.messageStore();
    }

    /** 摘要存储（委托 {@link #stores()}）。 */
    public SummaryStore summaryStore() {
        return stores.summaryStore();
    }

    /** 会话状态存储（委托 {@link #stores()}）。 */
    public SessionStateStore sessionStateStore() {
        return stores.sessionStateStore();
    }

    /** 会话租约存储（委托 {@link #stores()}）。 */
    public SessionLeaseStore sessionLeaseStore() {
        return stores.sessionLeaseStore();
    }

    /** 观测存储（委托 {@link #stores()}）。 */
    public ObservabilityStore observabilityStore() {
        return stores.observabilityStore();
    }

    /** 工作单元（委托 {@link #stores()}）。 */
    public UnitOfWork unitOfWork() {
        return stores.unitOfWork();
    }
}

package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunRegistry;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLog;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * impl-35 / spec 13 §stores-6：会话级联清理协调器——「删除会话 = 一次调用清干净」。
 *
 * <p>一次 {@link #deleteSession(String)} 依次清理（逐目标隔离，单个失败不跳过其余）：
 * message / summary / state / lease / observability 五槽 store →
 * （可选）RunRegistry / ToolCallLog 恢复设施 →（可选）外部贡献者
 * （spill 文件、embedding 缓存等，经 {@link #withContributor(String, Consumer)} 或
 * {@code RuntimeConfig.sessionCleanupContributors()} 挂接）。
 *
 * <p>失败聚合报告（{@link SessionCleanupResult}）：每目标 try/catch、ERROR 日志，
 * 全部尝试完毕后返回成功/失败清单——由调用方决定上抛策略；会话删除路径
 * （{@code AgentSession.delete()}）把首个失败上抛、其余 suppressed（与 impl-30 close 对齐）。
 *
 * <p>语义一致性由 {@code AbstractBuzhouStoresContractTest} 的 deleteSession 契约背书
 * （InMemory / JDBC / Redis 三实现全 store 无残留）。
 */
public final class SessionCleaner {

    private static final System.Logger LOGGER = System.getLogger(SessionCleaner.class.getName());

    private final BuzhouStores stores;
    private final RunRegistry runRegistry;
    private final ToolCallLog toolCallLog;
    private final List<SessionCleanupContributor> contributors;

    /** 仅五槽核心 store（既有组合的最小级联）。 */
    public SessionCleaner(BuzhouStores stores) {
        this(stores, null, null, List.of());
    }

    /** 五槽核心 store + 恢复设施（JDBC 组合 {@code createWithRecovery} 全量级联）。 */
    public SessionCleaner(BuzhouStores stores, RunRegistry runRegistry, ToolCallLog toolCallLog) {
        this(stores, runRegistry, toolCallLog, List.of());
    }

    private SessionCleaner(BuzhouStores stores, RunRegistry runRegistry, ToolCallLog toolCallLog,
                           List<SessionCleanupContributor> contributors) {
        this.stores = stores;
        this.runRegistry = runRegistry;
        this.toolCallLog = toolCallLog;
        this.contributors = List.copyOf(contributors);
    }

    /** 运行时装配入口：五槽 store + 配置携带的贡献者（RecoverySupport 挂接恢复设施用）。 */
    public static SessionCleaner of(BuzhouStores stores, List<SessionCleanupContributor> contributors) {
        return new SessionCleaner(stores, null, null, contributors == null ? List.of() : contributors);
    }

    /** 挂接外部贡献者（spill 文件 / embedding 缓存等），返回新实例（本实例不可变）。 */
    public SessionCleaner withContributor(String name, Consumer<String> deletion) {
        List<SessionCleanupContributor> merged = new ArrayList<>(contributors);
        merged.add(SessionCleanupContributor.of(name, deletion));
        return new SessionCleaner(stores, runRegistry, toolCallLog, merged);
    }

    /** 一次级联：逐目标隔离删除、失败聚合报告。目标槽位为 null 时跳过（不进报告）。 */
    public SessionCleanupResult deleteSession(String sessionId) {
        List<String> cleaned = new ArrayList<>();
        Map<String, RuntimeException> failures = new LinkedHashMap<>();
        if (stores != null) {
            runTarget("message-store", cleaned, failures,
                    () -> stores.messageStore().deleteSession(sessionId), stores.messageStore() != null);
            runTarget("summary-store", cleaned, failures,
                    () -> stores.summaryStore().deleteSession(sessionId), stores.summaryStore() != null);
            runTarget("session-state-store", cleaned, failures,
                    () -> stores.sessionStateStore().deleteSession(sessionId), stores.sessionStateStore() != null);
            runTarget("session-lease-store", cleaned, failures,
                    () -> stores.sessionLeaseStore().deleteSession(sessionId), stores.sessionLeaseStore() != null);
            runTarget("observability-store", cleaned, failures,
                    () -> stores.observabilityStore().deleteSession(sessionId), stores.observabilityStore() != null);
        }
        runTarget("run-registry", cleaned, failures,
                () -> runRegistry.deleteSession(sessionId), runRegistry != null);
        runTarget("tool-call-log", cleaned, failures,
                () -> toolCallLog.deleteSession(sessionId), toolCallLog != null);
        for (SessionCleanupContributor contributor : contributors) {
            runTarget(contributor.name(), cleaned, failures,
                    () -> contributor.deletion().accept(sessionId), true);
        }
        SessionCleanupResult result = new SessionCleanupResult(sessionId, cleaned, failures);
        if (!failures.isEmpty()) {
            LOGGER.log(System.Logger.Level.ERROR,
                    "会话级联清理部分失败（sessionId={0}，失败目标={1}，成功目标={2}）",
                    sessionId, failures.keySet(), cleaned);
        }
        return result;
    }

    private void runTarget(String name, List<String> cleaned, Map<String, RuntimeException> failures,
                           Runnable deletion, boolean present) {
        if (!present) {
            return;
        }
        try {
            deletion.run();
            cleaned.add(name);
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.ERROR,
                    "会话清理目标失败（已隔离，继续其余目标）：target={0}", name, e);
            failures.put(name, e);
        }
    }
}

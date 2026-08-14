package io.github.chyuan_cuihongyuan.buzhou.core.retention;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleaner;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryRunRegistry;
import io.github.chyuan_cuihongyuan.buzhou.core.internal.memory.InMemoryToolCallLog;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunStateSnapshot;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunStatus;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLogEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallOutcome;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.EventRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SpanRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StructuredSummary;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-37 / spec 13 §stores-6：RetentionSweeper 行为——封闭会话到期经 cleaner 级联清理、
 * 活动会话永不清、改短不追溯（notBefore）、批删封顶、手动触发（调度关）、失败隔离不中断、
 * 报告监听可观测。
 */
class RetentionSweeperTest {

    private static final Instant NOW = Instant.now();
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private final BuzhouStores stores = Buzhou.inMemoryStores();
    private final InMemoryRunRegistry runRegistry = new InMemoryRunRegistry();
    private final InMemoryToolCallLog toolCallLog = new InMemoryToolCallLog();
    private final SessionCleaner cleaner =
            new SessionCleaner(stores, runRegistry, toolCallLog);

    @Test
    void expiredClosedSessionsAreCascadedWhileActiveAndRecentOnesStay() {
        seedSession("expired", NOW.minus(Duration.ofDays(8)));   // 封闭 8 天 → 超 72h
        seedSession("recent-closed", NOW.minus(Duration.ofHours(1))); // 封闭 1h → 保留期内
        seedActiveSession("active");                              // 活动（未封闭）→ 永不清

        RetentionSweepReport report = newSweeper(SessionHistoryPolicy.defaults(),
                MaintenanceTrigger.defaults()).sweepOnce();

        assertThat(report.sessionsDeleted()).isEqualTo(1);
        assertThat(report.fullySucceeded()).isTrue();
        // 到期封闭会话全 store 清空
        assertThat(stores.messageStore().load("expired")).isEmpty();
        assertThat(stores.summaryStore().latest("expired")).isEmpty();
        assertThat(stores.sessionStateStore().getAll("expired")).isEmpty();
        assertThat(stores.observabilityStore().spansOfSession("expired")).isEmpty();
        assertThat(runRegistry.find("expired")).isEmpty();
        assertThat(toolCallLog.find("expired", "call-1")).isEmpty();
        // 保留期内的封闭会话与活动会话原封不动
        assertThat(stores.messageStore().load("recent-closed")).hasSize(1);
        assertThat(stores.messageStore().load("active")).hasSize(1);
        assertThat(stores.observabilityStore().spansOfSession("active")).hasSize(1);
    }

    @Test
    void shortenedPolicyDoesNotApplyRetroactively() {
        // 三天前把保留期从 72h 改短为 24h（notBefore=改短时刻）：
        // 改短之前封闭的旧会话按旧窗口不追溯（保留）；改短之后封闭且超新窗口者被清
        seedSession("closed-before-shorten", NOW.minus(Duration.ofDays(8)));
        seedSession("closed-after-shorten", NOW.minus(Duration.ofDays(2)));
        SessionHistoryPolicy shortened = new SessionHistoryPolicy(
                Duration.ofHours(24), NOW.minus(Duration.ofDays(3)));

        RetentionSweepReport report = newSweeper(shortened, MaintenanceTrigger.defaults()).sweepOnce();

        assertThat(report.sessionsDeleted()).isEqualTo(1);
        assertThat(stores.messageStore().load("closed-before-shorten")).hasSize(1); // 不追溯
        assertThat(stores.messageStore().load("closed-after-shorten")).isEmpty();   // 新窗口生效
    }

    @Test
    void batchLimitCapsDeletionsPerCycle() {
        // 年龄取 96h/80h：严格超 72h 保留期（恰好 72h 不算到期——isBefore 严格边界）、
        // 未到 7d 观测 TTL（观测 TTL 不先吃掉封闭锚点——与生产配置的相对序一致）
        seedSession("old-1", NOW.minus(Duration.ofHours(96)));
        seedSession("old-2", NOW.minus(Duration.ofHours(80)));
        RetentionSweeper sweeper = newSweeper(SessionHistoryPolicy.defaults(),
                new MaintenanceTrigger(null, null, 1, 1)); // 封顶 1：单周期只清一个

        assertThat(sweeper.sweepOnce().sessionsDeleted()).isEqualTo(1);
        assertThat(sweeper.sweepOnce().sessionsDeleted()).isEqualTo(1); // 下一周期清余量
        assertThat(stores.messageStore().load("old-1")).isEmpty();
        assertThat(stores.messageStore().load("old-2")).isEmpty();
    }

    @Test
    void manualSweepWorksWithoutSchedulingAndListenerObserves() {
        seedSession("expired", NOW.minus(Duration.ofDays(8)));
        RetentionSweeper sweeper = newSweeper(SessionHistoryPolicy.defaults(),
                MaintenanceTrigger.defaults());
        assertThat(sweeper.isRunning()).isFalse(); // 未启动调度——手动触发仍可兑现

        AtomicReference<RetentionSweepReport> seen = new AtomicReference<>();
        sweeper.addSweepListener(seen::set);
        RetentionSweepReport report = sweeper.sweepOnce();

        assertThat(report.sessionsDeleted()).isEqualTo(1);
        assertThat(seen.get()).isEqualTo(report); // 清理动作发报告（可观测）
    }

    @Test
    void failingPruneStepIsIsolatedAndCycleContinues() {
        // 观测 TTL 批删抛异常（封闭枚举/会话清理可用）
        InMemoryObservabilityStore failing = new InMemoryObservabilityStore() {
            @Override
            public int prune(ObservabilityTtl policy) {
                throw new IllegalStateException("ttl sweep down");
            }
        };
        // 封闭锚点（SESSION span）铺进 failing 实例——枚举从它读；事实数据仍铺共享 store
        failing.saveSpans(List.of(new SpanRecord("sp-expired", null, "expired", -1,
                "SESSION", "session", NOW.minus(Duration.ofDays(9)), NOW.minus(Duration.ofDays(8)),
                "OK", Map.of())));
        stores.messageStore().append("expired", List.of(msg("expired")));
        BuzhouStores failingStores = new BuzhouStores(stores.messageStore(), stores.summaryStore(),
                stores.sessionStateStore(), stores.sessionLeaseStore(), failing, stores.unitOfWork());
        RetentionSweeper sweeper = new RetentionSweeper(
                new SessionCleaner(failingStores, runRegistry, toolCallLog),
                failing, stores.summaryStore(), toolCallLog, runRegistry,
                SessionHistoryPolicy.defaults(), new ObservabilityTtl(null, null),
                3, Duration.ofDays(7), Duration.ofHours(24),
                MaintenanceTrigger.defaults(), Duration.ofHours(1), CLOCK);

        RetentionSweepReport report = sweeper.sweepOnce();

        assertThat(report.sessionsDeleted()).isEqualTo(1); // 会话保留照常兑现
        assertThat(report.failures()).isNotEmpty();
        assertThat(report.failures().getFirst()).contains("ttl sweep down");
        assertThat(stores.messageStore().load("expired")).isEmpty();
        assertThat(report.summaryVersionsPruned()).isZero(); // 其余步骤执行（本例无旧版本）
    }

    @Test
    void windowsPruneToolCallLogAndCompletedRuns() {
        // 窗口外（8 天前）条目清、窗口内（1 天前）保留；RUNNING run 永不窗口清理
        toolCallLog.append(new ToolCallLogEntry("w-1", "call-old", "echo", "h",
                ToolCallOutcome.COMPLETED, "ok", NOW.minus(Duration.ofDays(8))));
        toolCallLog.append(new ToolCallLogEntry("w-1", "call-new", "echo", "h",
                ToolCallOutcome.COMPLETED, "ok", NOW.minus(Duration.ofDays(1))));
        runRegistry.save(new RunStateSnapshot("w-run-done", "app", "agent", RunStatus.COMPLETED,
                1, 1, "owner", NOW.minus(Duration.ofDays(2))));
        runRegistry.save(new RunStateSnapshot("w-run-live", "app", "agent", RunStatus.RUNNING,
                1, 0, "owner", NOW.minus(Duration.ofDays(2))));

        RetentionSweepReport report = newSweeper(SessionHistoryPolicy.defaults(),
                MaintenanceTrigger.defaults()).sweepOnce();

        assertThat(report.toolCallLogPruned()).isEqualTo(1);
        assertThat(toolCallLog.find("w-1", "call-old")).isEmpty();
        assertThat(toolCallLog.find("w-1", "call-new")).isPresent();
        assertThat(report.completedRunsPruned()).isEqualTo(1);
        assertThat(runRegistry.find("w-run-done")).isEmpty();
        assertThat(runRegistry.find("w-run-live")).isPresent(); // 恢复巡检依赖在途快照
    }

    private RetentionSweeper newSweeper(SessionHistoryPolicy policy, MaintenanceTrigger trigger) {
        return new RetentionSweeper(cleaner, stores.observabilityStore(), stores.summaryStore(),
                toolCallLog, runRegistry, policy, new ObservabilityTtl(null, null),
                3, Duration.ofDays(7), Duration.ofHours(24), trigger, Duration.ofHours(1), CLOCK);
    }

    /** 封闭会话铺底：SESSION span（已结束）+ 全槽数据 + COMPLETED run + 工具日志。 */
    private void seedSession(String sessionId, Instant closedAt) {
        stores.observabilityStore().saveSpans(List.of(new SpanRecord(
                "sp-" + sessionId, null, sessionId, -1, "SESSION", "session",
                closedAt.minus(Duration.ofHours(1)), closedAt, "OK", Map.of())));
        stores.messageStore().append(sessionId, List.of(msg(sessionId)));
        stores.summaryStore().save(sessionId,
                new StructuredSummary(sessionId, 0, Map.of("P0", "a"), 9, closedAt));
        stores.sessionStateStore().put(sessionId,
                new StateEntry("k", "v", "hook", 1, null, closedAt));
        runRegistry.save(new RunStateSnapshot(sessionId, "app", "agent", RunStatus.COMPLETED,
                1, 1, "owner", closedAt));
        toolCallLog.append(new ToolCallLogEntry(sessionId, "call-1", "echo", "h",
                ToolCallOutcome.COMPLETED, "ok", closedAt));
    }

    /** 活动会话铺底：SESSION span 未结束（endedAt=null）+ 消息。 */
    private void seedActiveSession(String sessionId) {
        stores.observabilityStore().saveSpans(List.of(new SpanRecord(
                "sp-" + sessionId, null, sessionId, -1, "SESSION", "session",
                NOW.minus(Duration.ofMinutes(10)), null, "RUNNING", Map.of())));
        stores.messageStore().append(sessionId, List.of(msg(sessionId)));
    }

    private static BuzhouMessage msg(String sessionId) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, 1, 0, Role.USER,
                "content", List.of(), null, null, null, Map.of(), NOW);
    }
}

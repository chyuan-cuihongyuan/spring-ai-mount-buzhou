package io.github.chyuan_cuihongyuan.buzhou.core.retention;

import io.github.chyuan_cuihongyuan.buzhou.core.cleanup.SessionCleaner;
import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.BuzhouThreadFactory;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.RunRegistry;
import io.github.chyuan_cuihongyuan.buzhou.core.recovery.ToolCallLog;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ClosedSession;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.ObservabilityStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SummaryStore;
import org.springframework.context.SmartLifecycle;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * impl-37 / spec 13 §stores-6：保留策略族的后台执行器（RetentionSweeper）——「只进不出」终结。
 *
 * <p>每周期（默认 PT1H、可关）依次兑现，逐项隔离失败（WARN + 报告、不中断周期）：
 * <ol>
 *   <li><b>会话保留</b>（{@link SessionHistoryPolicy}）：枚举封闭会话（锚点=closedAt、
 *       活动会话永不清、notBefore 前封闭者不追溯），到期者经 {@link SessionCleaner}
 *       一次级联删除（含恢复设施/贡献者）；单周期批删限量 = {@link MaintenanceTrigger}
 *       公式（base + scaleFactor×总量，封顶 + 硬性下限兜底）；</li>
 *   <li><b>观测 TTL</b>（{@link ObservabilityTtl}）：过期 events/spans/snapshots 批删；</li>
 *   <li><b>摘要版本修剪</b>：每会话保留最近 K 版；</li>
 *   <li><b>ToolCallLog 窗口</b> / <b>RunRegistry COMPLETED 窗口</b>：窗口外删除。</li>
 * </ol>
 *
 * <p>调度关闭（{@code enabled=false} 或不自启动）时各策略仍可手动触发
 * （{@link #sweepOnce()}）。清理动作发报告（{@link #addSweepListener}）——可观测不静默。
 * SmartLifecycle：phase 按 {@code BuzhouLifecyclePhases.RETENTION}（core 停机后排空后台）。
 *
 * <p><b>锚点与观测 TTL 的相对序约定</b>：会话保留（默认 72h）应短于观测 TTL（默认 7d）
 * ——封闭会话先被级联删除、其观测流水随之清空；若长期停摆后重启（封闭已超两者），
 * 观测 TTL 可能先吃掉 SESSION span（锚点），该会话的观测流水随 TTL 消亡，
 * 事实台账残留由运维经 SessionCleaner 兜底。
 */
public class RetentionSweeper implements SmartLifecycle, AutoCloseable {

    private static final System.Logger LOGGER = System.getLogger(RetentionSweeper.class.getName());

    private final SessionCleaner cleaner;
    private final ObservabilityStore observability;
    private final SummaryStore summaries;
    private final ToolCallLog toolCallLog;
    private final RunRegistry runRegistry;
    private final SessionHistoryPolicy sessionHistory;
    private final ObservabilityTtl observabilityTtl;
    private final int summaryKeepVersions;
    private final Duration toolCallLogRetention;
    private final Duration runCompletedRetention;
    private final MaintenanceTrigger trigger;
    private final Clock clock;
    private final Duration sweepInterval;
    /** impl-37：SmartLifecycle 自启动开关（false = bean 存在但不排程——手动 sweepOnce 仍可用）。 */
    private final boolean autoStartup;
    private final List<Consumer<RetentionSweepReport>> listeners = new CopyOnWriteArrayList<>();

    private final AtomicBoolean running = new AtomicBoolean();
    private volatile ScheduledExecutorService scheduler;

    public RetentionSweeper(SessionCleaner cleaner,
                            ObservabilityStore observability,
                            SummaryStore summaries,
                            ToolCallLog toolCallLog,
                            RunRegistry runRegistry,
                            SessionHistoryPolicy sessionHistory,
                            ObservabilityTtl observabilityTtl,
                            int summaryKeepVersions,
                            Duration toolCallLogRetention,
                            Duration runCompletedRetention,
                            MaintenanceTrigger trigger,
                            Duration sweepInterval,
                            Clock clock) {
        this(cleaner, observability, summaries, toolCallLog, runRegistry, sessionHistory,
                observabilityTtl, summaryKeepVersions, toolCallLogRetention, runCompletedRetention,
                trigger, sweepInterval, clock, true);
    }

    public RetentionSweeper(SessionCleaner cleaner,
                            ObservabilityStore observability,
                            SummaryStore summaries,
                            ToolCallLog toolCallLog,
                            RunRegistry runRegistry,
                            SessionHistoryPolicy sessionHistory,
                            ObservabilityTtl observabilityTtl,
                            int summaryKeepVersions,
                            Duration toolCallLogRetention,
                            Duration runCompletedRetention,
                            MaintenanceTrigger trigger,
                            Duration sweepInterval,
                            Clock clock,
                            boolean autoStartup) {
        this.cleaner = cleaner;
        this.observability = observability;
        this.summaries = summaries;
        this.toolCallLog = toolCallLog;
        this.runRegistry = runRegistry;
        this.sessionHistory = sessionHistory == null ? SessionHistoryPolicy.defaults() : sessionHistory;
        this.observabilityTtl = observabilityTtl == null ? new ObservabilityTtl(null, null) : observabilityTtl;
        this.summaryKeepVersions = Math.max(1, summaryKeepVersions);
        this.toolCallLogRetention = toolCallLogRetention == null
                || toolCallLogRetention.isZero() || toolCallLogRetention.isNegative()
                ? Duration.ofDays(7) : toolCallLogRetention;
        this.runCompletedRetention = runCompletedRetention == null
                || runCompletedRetention.isZero() || runCompletedRetention.isNegative()
                ? Duration.ofHours(24) : runCompletedRetention;
        this.trigger = trigger == null ? MaintenanceTrigger.defaults() : trigger;
        this.sweepInterval = sweepInterval == null
                || sweepInterval.isZero() || sweepInterval.isNegative()
                ? Duration.ofHours(1) : sweepInterval;
        this.clock = clock == null ? Clock.systemUTC() : clock;
        this.autoStartup = autoStartup;
    }

    /** 清理动作监听者（每周期一份报告；可观测不静默）。 */
    public void addSweepListener(Consumer<RetentionSweepReport> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /** 单周期执行（手动触发入口——调度关闭时各策略仍可兑现）。 */
    public RetentionSweepReport sweepOnce() {
        Instant now = clock.instant();
        List<String> failures = new ArrayList<>();
        int sessionsDeleted = sweepExpiredSessions(now, failures);
        int observabilityPruned = runStep("observability-ttl",
                () -> observability == null ? 0 : observability.prune(observabilityTtl), failures);
        int summaryVersionsPruned = runStep("summary-prune",
                () -> summaries == null ? 0 : summaries.pruneVersions(summaryKeepVersions), failures);
        int toolCallLogPruned = runStep("tool-call-log-window",
                () -> toolCallLog == null ? 0 : toolCallLog.prune(now.minus(toolCallLogRetention)), failures);
        int completedRunsPruned = runStep("run-completed-window",
                () -> runRegistry == null ? 0 : runRegistry.pruneCompletedBefore(now.minus(runCompletedRetention)),
                failures);
        RetentionSweepReport report = new RetentionSweepReport(now, sessionsDeleted, observabilityPruned,
                summaryVersionsPruned, toolCallLogPruned, completedRunsPruned, failures);
        listeners.forEach(l -> {
            try {
                l.accept(report);
            } catch (RuntimeException e) {
                LOGGER.log(System.Logger.Level.WARNING, "保留清理报告监听者失败（已隔离）", e);
            }
        });
        return report;
    }

    /**
     * 会话保留：到期封闭会话（notBefore 之后的锚点起算超保留期）经 cleaner 级联删除。
     * 诚实边界：批删公式的「总量」取枚举窗口内的封闭数（≤cap）——窗口外的存量不影响
     * 本周期批量（周期间收敛，不引入全表 COUNT）。
     */
    private int sweepExpiredSessions(Instant now, List<String> failures) {
        if (observability == null || cleaner == null) {
            return 0;
        }
        try {
            // 枚举上界放最远（保留期前的都可能到期），notBefore 过滤由 policy.expired 完成
            List<ClosedSession> closed = observability.listClosedSessions(
                    now.minus(sessionHistory.retention()), trigger.cap());
            List<ClosedSession> expired = closed.stream()
                    .filter(c -> sessionHistory.expired(c.closedAt(), now))
                    .sorted(Comparator.comparing(ClosedSession::closedAt))
                    .toList();
            int batch = trigger.batchLimit(closed.size());
            int deleted = 0;
            for (ClosedSession candidate : expired) {
                if (deleted >= batch) {
                    break;
                }
                if (cleaner.deleteSession(candidate.sessionId()).fullyCleaned()) {
                    deleted++;
                } else {
                    failures.add("session-cascade:" + candidate.sessionId());
                }
            }
            return deleted;
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING, "会话保留清理失败（不中断周期）", e);
            failures.add("session-history:" + e);
            return 0;
        }
    }

    private int runStep(String step, java.util.function.IntSupplier work, List<String> failures) {
        try {
            return work.getAsInt();
        } catch (RuntimeException e) {
            LOGGER.log(System.Logger.Level.WARNING, "保留清理步骤失败（不中断周期）：step=" + step, e);
            failures.add(step + ":" + e);
            return 0;
        }
    }

    // ---- SmartLifecycle：低频后台调度（可关；关时不影响手动 sweepOnce） ----

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            scheduler = Executors.newSingleThreadScheduledExecutor(
                    BuzhouThreadFactory.virtual("retention-sweeper"));
            scheduler.scheduleWithFixedDelay(this::sweepQuietly,
                    sweepInterval.toMillis(), sweepInterval.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private void sweepQuietly() {
        try {
            sweepOnce();
        } catch (RuntimeException e) {
            // sweepOnce 内部已逐项隔离；此处只防调度线程被意外异常杀死
            LOGGER.log(System.Logger.Level.WARNING, "保留清理周期异常（调度继续）", e);
        }
    }

    @Override
    public void stop() {
        if (running.compareAndSet(true, false)) {
            ScheduledExecutorService current = scheduler;
            if (current != null) {
                current.shutdownNow();
                scheduler = null;
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    /** impl-37：{@code false} = bean 存在但 Spring 不自动排程（buzhou.retention.enabled=false）——手动 sweepOnce 仍可用。 */
    @Override
    public boolean isAutoStartup() {
        return autoStartup;
    }

    @Override
    public int getPhase() {
        return io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouLifecyclePhases.RETENTION;
    }

    @Override
    public void close() {
        stop();
    }
}

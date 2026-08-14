package io.github.chyuan_cuihongyuan.buzhou.guard.policy;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.BuzhouThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 策略热加载刷新器（impl-40 / spec 13 §T64）：{@link PolicyEngine} 的可刷新门面——
 *
 * <ul>
 *   <li><b>快照原子替换</b>：整份「规则 + revision + activatedAt」经 volatile 引用单点切换，
 *       并发 decide() 只见旧版或新版，<b>绝不部分生效</b>；</li>
 *   <li><b>etag 条件拉取</b>：内容 sha256 未变（null/304 语义）即跳过解析；</li>
 *   <li><b>失败沿用旧版</b>：来源不可读 / JSON 解析失败 → 保留当前快照 + WARN + 失败计数
 *       （策略变更永不因坏文件半生效）；</li>
 *   <li><b>轮询可关</b>：{@code refreshInterval <= 0} = 仅启动加载（静态策略零开销）；
 *       默认 PT30S，经守护线程轮询（BuzhouThreadFactory 命名 + 异常隔离）；</li>
 *   <li><b>provenance</b>：decision 携带 revision（etag）与 activatedAt——决策可溯源到规则版本。</li>
 * </ul>
 */
public final class PolicyRefresher implements PolicyEngine, AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(PolicyRefresher.class);

    private record SnapshotState(List<PolicyDecision.Rule> rules, String revision,
                                 Instant activatedAt, EmbeddedPolicyEngine engine) {
    }

    private final PolicySource source;
    private final Duration refreshInterval;
    private final ScheduledExecutorService scheduler;

    private volatile SnapshotState current;
    private final AtomicLong refreshSuccessCount = new AtomicLong();
    private final AtomicLong refreshFailureCount = new AtomicLong();
    private final AtomicLong refreshNoChangeCount = new AtomicLong();

    /**
     * @param refreshInterval 轮询间隔；{@code <= 0} 或 null = 关闭轮询（仅启动加载）
     * @throws IllegalStateException 启动首载失败（fail-fast：策略门启用而来源不可用须显式暴露）
     */
    public PolicyRefresher(PolicySource source, Duration refreshInterval) {
        this.source = source;
        this.refreshInterval = refreshInterval == null ? Duration.ZERO : refreshInterval;
        this.current = loadInitial();
        if (this.refreshInterval.isZero() || this.refreshInterval.isNegative()) {
            this.scheduler = null;
            LOG.info("guard policy 静态加载完成（rules={}，revision={}，轮询关闭）",
                    current.rules().size(), shortRevision(current.revision()));
        } else {
            this.scheduler = Executors.newSingleThreadScheduledExecutor(
                    BuzhouThreadFactory.platform("guard-policy-refresh"));
            this.scheduler.scheduleAtFixedRate(this::refreshQuietly,
                    this.refreshInterval.toMillis(), this.refreshInterval.toMillis(),
                    TimeUnit.MILLISECONDS);
            LOG.info("guard policy 静态加载完成（rules={}，revision={}）并开启轮询（interval={}）",
                    current.rules().size(), shortRevision(current.revision()), this.refreshInterval);
        }
    }

    private SnapshotState loadInitial() {
        PolicySource.Snapshot snapshot = source.load(null);
        Instant activatedAt = Instant.now();
        LOG.info("guard policy 加载（source={}，rules={}）", source.description(),
                snapshot.rules().size());
        return new SnapshotState(snapshot.rules(), snapshot.etag(), activatedAt,
                new EmbeddedPolicyEngine(snapshot.rules()));
    }

    @Override
    public PolicyDecision decide(PolicyDecision.Input input) {
        SnapshotState state = current;
        PolicyDecision decision = state.engine().decide(input);
        return new PolicyDecision(decision.action(), decision.reason(),
                state.revision(), state.activatedAt());
    }

    /** 手动/轮询刷新入口（异常自兜，轮询线程永不因单次失败终止）。 */
    public synchronized void refresh() {
        SnapshotState state = current;
        try {
            PolicySource.Snapshot snapshot = source.load(state.revision());
            if (snapshot == null) {
                refreshNoChangeCount.incrementAndGet();
                return;
            }
            Instant activatedAt = Instant.now();
            SnapshotState next = new SnapshotState(snapshot.rules(), snapshot.etag(),
                    activatedAt, new EmbeddedPolicyEngine(snapshot.rules()));
            this.current = next; // 原子替换：旧快照引用即刻整体让位
            refreshSuccessCount.incrementAndGet();
            LOG.info("guard policy 热加载生效（source={}，rules={}，revision={} → {}）",
                    source.description(), next.rules().size(),
                    shortRevision(state.revision()), shortRevision(next.revision()));
        } catch (RuntimeException e) {
            refreshFailureCount.incrementAndGet();
            LOG.warn("guard policy 刷新失败——沿用当前快照（revision={}，rules={}）：{}",
                    shortRevision(state.revision()), state.rules().size(), e.getMessage());
        }
    }

    private void refreshQuietly() {
        try {
            refresh();
        } catch (RuntimeException e) {
            // scheduleAtFixedRate 的任务抛出会终止后续调度——此处物理兜底（理论不可达）
            refreshFailureCount.incrementAndGet();
            LOG.error("guard policy 轮询任务异常（调度不应中断）", e);
        }
    }

    public String revision() {
        return current.revision();
    }

    public Instant activatedAt() {
        return current.activatedAt();
    }

    public int ruleCount() {
        return current.rules().size();
    }

    public boolean pollingEnabled() {
        return scheduler != null;
    }

    public long refreshSuccessCount() {
        return refreshSuccessCount.get();
    }

    public long refreshFailureCount() {
        return refreshFailureCount.get();
    }

    public long refreshNoChangeCount() {
        return refreshNoChangeCount.get();
    }

    @Override
    public void close() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    private static String shortRevision(String etag) {
        return etag == null ? "?" : etag.substring(0, Math.min(12, etag.length()));
    }
}

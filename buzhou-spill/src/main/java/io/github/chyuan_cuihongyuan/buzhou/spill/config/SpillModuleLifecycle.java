package io.github.chyuan_cuihongyuan.buzhou.spill.config;

import io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouLifecyclePhases;
import io.github.chyuan_cuihongyuan.buzhou.core.retention.RetentionSweeper;
import io.github.chyuan_cuihongyuan.buzhou.spill.DiskSpillStore;
import org.springframework.context.SmartLifecycle;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * spill 生命周期（impl-30 / spec 13 §core-1：phase =
 * {@link BuzhouLifecyclePhases#SPILL}，core 停机会话之后、guard/store 之前）。
 *
 * <p>impl-38 / spec 13 §growth-8 落地接线：
 * <ul>
 *   <li><b>启动孤儿扫描</b>：start 时以 live 会话集合（观测 store 会话汇总）比对落盘
 *       目录——引用会话不存在的文件报告（INFO 日志）并清理（幂等）；</li>
 *   <li><b>spill TTL 调度</b>：{@code deleteExpired(now, ttl)} 经
 *       {@link RetentionSweeper#addSweepStep} 挂进保留周期（默认 PT1H）——无 sweeper
 *       bean 时跳过挂接（TTL 退化为仅启动扫描，诚实降级）。</li>
 * </ul>
 */
public class SpillModuleLifecycle implements SmartLifecycle {

    private static final System.Logger LOGGER =
            System.getLogger(SpillModuleLifecycle.class.getName());

    private final DiskSpillStore store;
    private final Duration retentionTtl;
    private final Supplier<Set<String>> liveSessions;
    private final RetentionSweeper sweeper;
    private final AtomicBoolean running = new AtomicBoolean();

    public SpillModuleLifecycle(DiskSpillStore store, Duration retentionTtl,
                                Supplier<Set<String>> liveSessions, RetentionSweeper sweeper) {
        this.store = store;
        this.retentionTtl = retentionTtl == null || retentionTtl.isZero() || retentionTtl.isNegative()
                ? Duration.ofHours(24) : retentionTtl;
        this.liveSessions = liveSessions == null ? Set::of : liveSessions;
        this.sweeper = sweeper;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            Set<String> live = liveSessions.get();
            int orphans = store.sweepOrphans(live);
            if (orphans > 0) {
                LOGGER.log(System.Logger.Level.INFO,
                        "spill 启动孤儿扫描：清理引用会话不存在的文件 {0} 个（live 会话 {1} 个）",
                        orphans, live.size());
            }
            if (sweeper != null) {
                sweeper.addSweepStep("spill-ttl",
                        () -> store.deleteExpired(Instant.now(), retentionTtl));
            }
        }
    }

    @Override
    public void stop() {
        running.set(false);
    }

    @Override
    public boolean isRunning() {
        return running.get();
    }

    @Override
    public int getPhase() {
        return BuzhouLifecyclePhases.SPILL;
    }
}

package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.BuzhouThreadFactory;
import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.LeaderElector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * store fsck 定时巡检（spec 538 / T827——341 选主扩散第三弹：331 选主门
 * 接 StoreFsck——对账面从「手工触发」升级「定时巡检」）。周期 run（只读）
 * → findings > 0 即 WARN + 计数（不自动修复——repair 仍归手工面，删除
 * 动作必须显式）。
 *
 * <p>诚实边界：只读巡检（不自动 repair）；elector 缺席 = 无门单实例跑
 * （与 ArchivePurgeJob 同语义——有 bean 即门）。
 */
public class StoreFsckHousekeeper implements SmartLifecycle {

    private static final Logger LOG = LoggerFactory.getLogger(StoreFsckHousekeeper.class);

    private final BuzhouStores stores;
    private final LeaderElector elector;
    private final Duration interval;

    private final AtomicBoolean running = new AtomicBoolean();
    private volatile ScheduledExecutorService scheduler;
    private final AtomicLong runs = new AtomicLong();
    private final AtomicLong totalFindings = new AtomicLong();
    private final AtomicLong skippedNotLeader = new AtomicLong();
    /** spec 548 / T827：最近一次巡检 findings 数（健康详情）。 */
    private volatile int lastFindings = -1;

    public StoreFsckHousekeeper(BuzhouStores stores, LeaderElector elector, Duration interval) {
        this.stores = stores;
        this.elector = elector;
        this.interval = interval == null ? Duration.ofHours(6) : interval;
    }

    @Override
    public void start() {
        if (running.compareAndSet(false, true)) {
            scheduler = Executors.newSingleThreadScheduledExecutor(
                    BuzhouThreadFactory.platform("buzhou-store-fsck"));
            scheduler.scheduleAtFixedRate(this::gatedTick,
                    interval.toMillis(), interval.toMillis(), TimeUnit.MILLISECONDS);
        }
    }

    /** tick 异常隔离（housekeeper 族同法）。 */
    private void gatedTick() {
        try {
            if (elector != null && !elector.tryAcquireOrRenew().leader()) {
                skippedNotLeader.incrementAndGet();
                return;
            }
            evaluateOnce();
        } catch (RuntimeException ignored) {
            // 防调度线程被意外异常杀死
        }
    }

    /** 单次巡检（只读）：findings > 0 WARN + 计数；不自动修复。 */
    public synchronized StoreIntegrityReport evaluateOnce() {
        StoreIntegrityReport report = StoreFsck.run(stores);
        runs.incrementAndGet();
        int findings = report.findings().size();
        totalFindings.addAndGet(findings);
        lastFindings = findings;
        BuzhouMetricsHolder.metrics().counter("buzhou.fsck.runs");
        if (findings > 0) {
            BuzhouMetricsHolder.metrics().counter("buzhou.fsck.findings");
            LOG.warn("store fsck 巡检发现 {} 处不一致（只读报告——修复走手工 repair）", findings);
        }
        return report;
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

    public long runs() {
        return runs.get();
    }

    public long totalFindings() {
        return totalFindings.get();
    }

    public long skippedNotLeader() {
        return skippedNotLeader.get();
    }

    /** 最近一次巡检 findings 数（-1 = 尚未巡检）。 */
    public int lastFindings() {
        return lastFindings;
    }
}

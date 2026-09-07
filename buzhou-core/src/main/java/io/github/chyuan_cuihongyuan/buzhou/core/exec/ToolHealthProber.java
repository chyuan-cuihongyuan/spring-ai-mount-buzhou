package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * 工具健康探测器（spec 165 / T527，Consul health check 借鉴）：宿主注册
 * per-tool 轻量探针（框架不知道怎么探——分层诚实）；probeOnce 聚合
 * UP/DOWN + consecutiveDown（探针异常/false = DOWN 不上抛）；<b>翻转才通知</b>
 * （不刷屏）；DOWN 计数。与工具熔断（131）互补：被动结局 vs 主动提前。
 */
public final class ToolHealthProber {

    /** 工具状态。 */
    public enum Status { UP, DOWN }

    /** 探测结果行。 */
    public record ToolStatus(Status status, int consecutiveDown) {
    }

    private final Map<String, ProbeHolder> probes = new LinkedHashMap<>();
    private final List<BiConsumer<String, ToolStatus>> listeners = new CopyOnWriteArrayList<>();
    private volatile ScheduledExecutorService scheduler;

    private static final class ProbeHolder {
        final Callable<Boolean> probe;
        volatile Status last = Status.UP;
        volatile int consecutiveDown;
        volatile boolean probed;

        ProbeHolder(Callable<Boolean> probe) {
            this.probe = probe;
        }
    }

    /** 注册探针（重复注册覆盖；probe 非空）。 */
    public synchronized void register(String toolName, Callable<Boolean> probe) {
        if (toolName == null || toolName.isBlank() || probe == null) {
            throw new IllegalArgumentException("toolName/probe 非空");
        }
        probes.put(toolName, new ProbeHolder(probe));
    }

    /** 注销。 */
    public synchronized void unregister(String toolName) {
        probes.remove(toolName);
    }

    /** 翻转监听（仅状态变化时回调）。 */
    public void onChange(BiConsumer<String, ToolStatus> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    /**
     * 聚合探测一轮：逐工具执行探针（串行——数十量级轻探针），返回状态表
     * （稳定序）；探针异常/false = DOWN（连败累计）；恢复清零。DOWN 计数。
     */
    public synchronized Map<String, ToolStatus> probeOnce() {
        Map<String, ToolStatus> out = new TreeMap<>();
        probes.forEach((name, holder) -> {
            boolean healthy;
            try {
                healthy = Boolean.TRUE.equals(holder.probe.call());
            } catch (RuntimeException e) {
                healthy = false; // 探活异常 = DOWN（不上抛——探测面不放大故障）
            } catch (Exception e) {
                healthy = false;
            }
            Status now = healthy ? Status.UP : Status.DOWN;
            if (healthy) {
                holder.consecutiveDown = 0;
            } else {
                holder.consecutiveDown++;
                io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder
                        .metrics().counter("buzhou.tool-probe.down", 1, "tool", name);
            }
            boolean flipped = holder.probed && holder.last != now;
            holder.last = now;
            holder.probed = true;
            ToolStatus status = new ToolStatus(now, holder.consecutiveDown);
            out.put(name, status);
            if (flipped) {
                listeners.forEach(l -> l.accept(name, status));
            }
        });
        return out;
    }

    /** 自调度（interval 周期 probeOnce；重复 start 拒绝）。 */
    public synchronized void start(java.time.Duration interval) {
        if (scheduler != null) {
            throw new IllegalStateException("探测器已在调度中");
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(
                io.github.chyuan_cuihongyuan.buzhou.core.concurrent.BuzhouThreadFactory
                        .platform("buzhou-tool-probe"));
        scheduler.scheduleAtFixedRate(this::probeOnce, interval.toMillis(),
                interval.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 最近已知状态快照（不主动探测——健康面/观测读取用；spec 305 / impl-328）。
     * 未探测过的注册项记 UP（未探视同可用——诚实边界：探测前无 DOWN 证据）。
     */
    public synchronized Map<String, ToolStatus> lastKnown() {
        Map<String, ToolStatus> out = new TreeMap<>();
        probes.forEach((name, holder) -> out.put(name,
                new ToolStatus(holder.probed ? holder.last : Status.UP, holder.consecutiveDown)));
        return out;
    }

    /** 停调度（幂等）。 */
    public synchronized void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }
}

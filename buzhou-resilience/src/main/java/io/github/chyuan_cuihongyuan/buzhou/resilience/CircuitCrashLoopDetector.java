package io.github.chyuan_cuihongyuan.buzhou.resilience;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 断路器 crash-loop 检测（spec 811 / T1123，k8s CrashLoopBackOff 思想）：
 * 模型断路器短窗内反复 OPEN（opensInWindow ≥ {@code minOpens}）标记为
 * looping——与单次跳闸不同，crash-loop 意味着「恢复即再炸」的系统性故障
 * （坏凭据/超载模型），应停止反复试探。
 *
 * <p>语义（k8s 对齐）：looping 为<b>闩锁态</b>——窗口滑过不自动解除，唯
 * {@link #recordRecovery}（等价容器成功运行）显式清除；每次「进入 looping」
 * 计 {@code loopsDetected}（反复炸反复计）。模型封顶 {@value #MAX_MODELS}
 * （truncated 如实）。旁路读数：由变迁记录点（702 journal 同挂点）喂 OPEN/
 * HALF_OPEN→CLOSED 恢复事件——不改断路器行为。
 */
public final class CircuitCrashLoopDetector {

    /** 模型数封顶。 */
    public static final int MAX_MODELS = 32;

    /** 单模型循环状态行。 */
    public record LoopState(String model, int opensInWindow, boolean looping, long loopsDetected) {
    }

    private static final class ModelState {
        final Deque<Long> openTimes = new ArrayDeque<>();
        volatile boolean looping;
        long loopsDetected;
    }

    private final int minOpens;
    private final long windowMillis;
    private final Map<String, ModelState> models = new ConcurrentHashMap<>();
    private volatile boolean truncated;

    // —— spec 1056 / impl 808：类级水位读面（kube-state-metrics crashloop 事件总账思想；
    // 静态面理由同 R46–R55 先例）。口径诚实：null/空白模型不落入任何桶。
    private static final AtomicLong OPENS_RECORDED = new AtomicLong();
    private static final AtomicLong OPENS_TRUNCATED = new AtomicLong();
    private static final AtomicLong LOOPS_DETECTED = new AtomicLong();
    private static final AtomicLong RECOVERIES_RECORDED = new AtomicLong();

    /** 崩循环探测器类级水位快照（spec 1056）。 */
    public record CrashLoopWatchStats(long opensRecorded, long opensTruncated,
                                      long loopsDetected, long recoveriesRecorded) {
    }

    /** 只读快照。 */
    public static CrashLoopWatchStats stats() {
        return new CrashLoopWatchStats(OPENS_RECORDED.get(), OPENS_TRUNCATED.get(),
                LOOPS_DETECTED.get(), RECOVERIES_RECORDED.get());
    }

    /** 测试专用归零（生产禁用——计数器是进程生命周期水位）。 */
    public static void resetForTest() {
        OPENS_RECORDED.set(0);
        OPENS_TRUNCATED.set(0);
        LOOPS_DETECTED.set(0);
        RECOVERIES_RECORDED.set(0);
    }

    public CircuitCrashLoopDetector(int minOpens, long windowMillis) {
        if (minOpens < 2) {
            throw new IllegalArgumentException("minOpens 必须 >= 2（当前 " + minOpens + "）");
        }
        if (windowMillis < 1) {
            throw new IllegalArgumentException("windowMillis 必须 >= 1（当前 " + windowMillis + "）");
        }
        this.minOpens = minOpens;
        this.windowMillis = windowMillis;
    }

    /** 记录一次 OPEN 跳闸（窗口内达 minOpens 即置闩锁态——仅在未闩时计数）。 */
    public void recordOpen(String model, long atEpochMs) {
        if (model == null || model.isBlank()) {
            return;
        }
        ModelState state = models.get(model);
        if (state == null) {
            if (models.size() >= MAX_MODELS) {
                truncated = true;
                OPENS_TRUNCATED.incrementAndGet();
                return;
            }
            state = models.computeIfAbsent(model, k -> new ModelState());
        }
        OPENS_RECORDED.incrementAndGet();
        synchronized (state) {
            state.openTimes.addLast(atEpochMs);
            while (!state.openTimes.isEmpty() && atEpochMs - state.openTimes.peekFirst() > windowMillis) {
                state.openTimes.pollFirst();
            }
            if (state.openTimes.size() >= minOpens && !state.looping) {
                state.looping = true;
                state.loopsDetected++;
                LOOPS_DETECTED.incrementAndGet();
            }
        }
    }

    /** 记录恢复（HALF_OPEN→CLOSED）——清除闩锁态与窗口（k8s 成功运行语义）。 */
    public void recordRecovery(String model, long atEpochMs) {
        ModelState state = model == null ? null : models.get(model);
        if (state == null) {
            return;
        }
        RECOVERIES_RECORDED.incrementAndGet();
        synchronized (state) {
            state.openTimes.clear();
            state.looping = false;
        }
    }

    /** 该模型当前是否处于 crash-loop 闩锁态。 */
    public boolean isLooping(String model) {
        ModelState state = model == null ? null : models.get(model);
        return state != null && state.looping;
    }

    /** 只读快照（按模型名典序）。 */
    public List<LoopState> snapshot() {
        List<LoopState> out = new ArrayList<>();
        models.keySet().stream().sorted().forEach(model -> {
            ModelState state = models.get(model);
            synchronized (state) {
                out.add(new LoopState(model, state.openTimes.size(), state.looping, state.loopsDetected));
            }
        });
        return out;
    }

    public boolean truncated() {
        return truncated;
    }
}

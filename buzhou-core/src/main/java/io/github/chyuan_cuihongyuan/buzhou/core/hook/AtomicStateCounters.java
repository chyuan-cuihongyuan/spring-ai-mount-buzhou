package io.github.chyuan_cuihongyuan.buzhou.core.hook;

import io.github.chyuan_cuihongyuan.buzhou.core.hook.SessionStateHandle;

import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * 会话 state 计数原子写助手（spec 62 §A / T275 / effort#22，范式提炼自 spec 56
 * SessionQuotaHook）：值形态无关的<b>进度检测 CAS</b>——读 raw → nextOf(raw) 算新值 →
 * {@code compareAndSwap(raw, next)}；失败后值仍在变（他人有进展）即继续重试（不丢
 * 计数）；仅值<b>停滞</b>满 {@value #MAX_STALLED_ATTEMPTS} 次（存储异常/对抗性失败）
 * 才回退 last-write 覆写 + {@code onFallback} 回调观测（宁可少记、不误拦截、不崩溃）。
 *
 * <p>值形态由调用方决定（纯数字 / {@code day:count} 日窗……）——nextOf 必须幂等于
 * 同一 raw（重试间不重复累计）。跨实例原子性由底层 store 的 compareAndSwap 覆写承诺
 * （内存/JDBC/池化 Redis 真原子；默认实现仅单实例——调用方自持 JVM 会话锁兜底）。
 *
 * <p>spec 707 / T965：自 {@code core.internal.hook} 迁出——跨模块复用类不入
 * internal（边界守卫 ModuleBoundaryGuardTest 口径）。
 */
public final class AtomicStateCounters {

    /** 停滞判定上限（连续读到相同 raw 的尝试数——运行中正常竞争不会触发）。 */
    public static final int MAX_STALLED_ATTEMPTS = 16;

    private AtomicStateCounters() {
    }

    /**
     * CAS 写并返回最终 raw 值（成功路径 = nextOf 的产物；回退路径 = 回退覆写的值）。
     *
     * @param state      会话 state 门面（CAS 透传底层 store）
     * @param key        计数键
     * @param nextOf     raw → 新值（幂等：同一 raw 多次调用产出相同结果）
     * @param onFallback 停滞回退发生时回调（可 null——静默回退）
     */
    public static String swapValue(SessionStateHandle state, String key,
            UnaryOperator<String> nextOf, Runnable onFallback) {
        String prevRaw = "unset-sentinel";
        int stalled = 0;
        while (stalled < MAX_STALLED_ATTEMPTS) {
            String raw = state.get(key, String.class).orElse(null);
            stalled = Objects.equals(raw, prevRaw) ? stalled + 1 : 0;
            prevRaw = raw;
            String next = nextOf.apply(raw);
            if (state.compareAndSwap(key, raw, next)) {
                return next;
            }
            Thread.yield(); // 礼让抢占方，缩短重试方饥饿窗口
        }
        if (onFallback != null) {
            onFallback.run();
        }
        String fallbackNext = nextOf.apply(state.get(key, String.class).orElse(null));
        state.put(key, fallbackNext);
        return fallbackNext;
    }
}

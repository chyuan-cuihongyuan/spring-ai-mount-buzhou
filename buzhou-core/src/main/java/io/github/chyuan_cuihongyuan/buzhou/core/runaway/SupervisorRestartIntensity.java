package io.github.chyuan_cuihongyuan.buzhou.core.runaway;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.LongSupplier;

/**
 * 监督者重启强度（spec 4025 / T6051 / impl 2126）——重启风暴升级
 * 思想（Erlang/OTP supervisor max_restarts/max_intensity）：监督者
 * 允许滑窗内至多 maxRestarts 次子重启——**越限即升级**（OTP 语义：
 * 终止全部子进程并停监督者——「无限重启循环把故障刷成噪音」病
 * 的熔断升级）；越限闩锁保持直到显式开新纪元（reset——重启的
 * 监督者从头计）。
 *
 * <p>时钟可注入（确定性回放）；restartsInWindow 读数可审计。
 * 与 TurnStallWatchdog（单轮失速）互补：本件管**重启频率**的
 * 累积判定。
 */
public final class SupervisorRestartIntensity {

    private final int maxRestarts;
    private final long windowMillis;
    private final LongSupplier clockMillis;
    private final Deque<Long> restarts = new ArrayDeque<>();
    private boolean exceeded;

    /** 定构（maxRestarts≥1、windowMillis≥1、时钟非 null 否则 fail-fast）。 */
    public SupervisorRestartIntensity(int maxRestarts, long windowMillis, LongSupplier clockMillis) {
        if (maxRestarts < 1 || windowMillis < 1 || clockMillis == null) {
            throw new IllegalArgumentException("max≥1 / window≥1 / clock 非空："
                    + maxRestarts + "/" + windowMillis);
        }
        this.maxRestarts = maxRestarts;
        this.windowMillis = windowMillis;
        this.clockMillis = clockMillis;
    }

    /** 记录一次子重启（滑窗淘汰后入账；越限即闩锁升级）。 */
    public void onRestart() {
        long now = clockMillis.getAsLong();
        prune(now);
        restarts.addLast(now);
        if (restarts.size() > maxRestarts) {
            exceeded = true;   // OTP：强度越限——升级停机（闩锁）
        }
    }

    /** 越限判定（闩锁——reset 前恒真）。 */
    public boolean exceeded() {
        return exceeded;
    }

    /** 当前滑窗内重启数（先淘汰再读）。 */
    public int restartsInWindow() {
        prune(clockMillis.getAsLong());
        return restarts.size();
    }

    /** 开新纪元（重启的监督者从头计——闩锁与滑窗双清）。 */
    public void reset() {
        restarts.clear();
        exceeded = false;
    }

    /** maxRestarts 读数。 */
    public int maxRestarts() {
        return maxRestarts;
    }

    private void prune(long now) {
        while (!restarts.isEmpty() && now - restarts.peekFirst() >= windowMillis) {
            restarts.pollFirst();
        }
    }
}

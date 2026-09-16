package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * 同步副本追踪器（spec 2037 / T3175 / impl 1588）——Kafka ISR（in-sync
 * replicas）思想：副本是否同步由**追上时刻**判定——lastCaughtUp 距今
 * 超滞后阈值即失同步（剔除出 ISR），重新追上（落后区间追平）即回归；
 * ISR 收缩事件计数显形（收缩越频繁 = 下游消费力越紧）。判定与回归
 * 都由调用方注入时刻（确定性可回放）。
 *
 * <p>synchronized 小临界区；注册序稳定。
 */
public final class InSyncTracker {

    private final long lagThresholdMillis;
    private final Map<String, Long> lastCaughtUpAt = new HashMap<>();
    private final Set<String> members = new LinkedHashSet<>();
    private long shrinkEvents;
    private int lastIsrSize = -1;

    /** 契约：lagThresholdMillis &gt; 0（fail-fast）。 */
    public InSyncTracker(long lagThresholdMillis) {
        if (lagThresholdMillis <= 0) {
            throw new IllegalArgumentException("lagThresholdMillis 须 > 0：" + lagThresholdMillis);
        }
        this.lagThresholdMillis = lagThresholdMillis;
    }

    /** 注册成员（初始视为同步——lastCaughtUp=0 起算）。契约：member 非空非重复。 */
    public synchronized void register(String member) {
        if (member == null || member.isBlank()) {
            throw new IllegalArgumentException("member 不能为空");
        }
        if (!members.add(member)) {
            throw new IllegalArgumentException("成员已注册：" + member);
        }
        lastCaughtUpAt.put(member, 0L);
    }

    /** 记成员追上（消费进度齐平）。契约：member 已注册、now ≥ 0。 */
    public synchronized void caughtUp(String member, long nowMillis) {
        Long prev = lastCaughtUpAt.get(member);
        if (prev == null) {
            throw new IllegalArgumentException("成员未注册：" + member);
        }
        if (nowMillis < 0) {
            throw new IllegalArgumentException("nowMillis 须 ≥ 0：" + nowMillis);
        }
        lastCaughtUpAt.put(member, Math.max(prev, nowMillis));
    }

    /** 成员此刻是否同步：lastCaughtUp 距今 < 阈值。未注册 false。 */
    public synchronized boolean isInSync(String member, long nowMillis) {
        if (nowMillis < 0) {
            throw new IllegalArgumentException("nowMillis 须 ≥ 0：" + nowMillis);
        }
        Long at = lastCaughtUpAt.get(member);
        return at != null && nowMillis - at < lagThresholdMillis;
    }

    /**
     * 当前同步集（ISR 快照，注册序）：仅含同步成员。若 ISR 较上次快照
     * 收缩（有成员掉出）计 shrinkEvent 一次（扩张不计——收缩才是风险
     * 信号）。
     */
    public synchronized Set<String> inSyncSet(long nowMillis) {
        Set<String> isr = new LinkedHashSet<>();
        members.forEach(m -> {
            if (isInSync(m, nowMillis)) {
                isr.add(m);
            }
        });
        if (isr.size() < lastIsrSize) {
            shrinkEvents++;
        }
        lastIsrSize = isr.size();
        return isr;
    }

    /** ISR 收缩事件计数（消费力紧张显形）。 */
    public synchronized long shrinkEvents() {
        return shrinkEvents;
    }

}

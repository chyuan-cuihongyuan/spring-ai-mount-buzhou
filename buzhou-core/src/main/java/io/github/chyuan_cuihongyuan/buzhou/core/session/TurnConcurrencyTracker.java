package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 跨会话轮次并发水位观察者（spec 1400 / T2101 / impl 1053）——HikariCP 池读面
 * （active/idle + 历史峰值）思想：同一实例经
 * {@link SessionAssemblyContext#addObserver(SessionObserver)} 注册到全部会话后，
 * 聚合出应用级在途轮次读数。会话内轮次单飞（spec 40 §B：CAS 0→1 同会话不并发），
 * 本读面的并发维度天然是<b>跨会话</b>。
 *
 * <p>守恒式：{@code started = okFinished + failed + active}——每轮恰派发一次
 * onTurnStart，终结恰落 onTurnEnd（成功/护栏拒绝）或 onTurnError（异常）之一；
 * guard-block 轮的终结回调补派见 spec 1400 同轮 DefaultAgentSession 修复。
 * 独立原子量在并发收尾下存在瞬态不等式窗口，最终守恒（快照读面不作强一致承诺）。
 *
 * <p>回调在会话主线程同步执行，全部为无锁原子记账，满足 SessionObserver 轻量契约。
 */
public final class TurnConcurrencyTracker implements SessionObserver {

    private final AtomicInteger active = new AtomicInteger();
    private final AtomicInteger peakActive = new AtomicInteger();
    private final AtomicLong started = new AtomicLong();
    private final AtomicLong okFinished = new AtomicLong();
    private final AtomicLong failed = new AtomicLong();

    @Override
    public void onTurnStart(int turnSeq, String userInput) {
        started.incrementAndGet();
        int now = active.incrementAndGet();
        peakActive.accumulateAndGet(now, Math::max);
    }

    @Override
    public void onTurnEnd(int turnSeq, String finalReply) {
        okFinished.incrementAndGet();
        decrementActive();
    }

    @Override
    public void onTurnError(int turnSeq, Throwable error) {
        failed.incrementAndGet();
        decrementActive();
    }

    /** 终结回调重复派发的防御（session 侧 finalized 守卫已兜一层）——活跃计数不为负。 */
    private void decrementActive() {
        int seen;
        do {
            seen = active.get();
            if (seen == 0) {
                return;
            }
        } while (!active.compareAndSet(seen, seen - 1));
    }

    /** 只读快照：活跃/峰值水位 + 生命周期三总量（守恒式见类注）。 */
    public Snapshot stats() {
        return new Snapshot(active.get(), peakActive.get(),
                started.get(), okFinished.get(), failed.get());
    }

    /** 测试归零口：装配级单例实例的 reset 注入点（BuzhouMetricsHolder 先例）。 */
    public void resetForTest() {
        active.set(0);
        peakActive.set(0);
        started.set(0);
        okFinished.set(0);
        failed.set(0);
    }

    /**
     * @param active      当前在途轮次（跨会话聚合）
     * @param peakActive  进程生命周期内活跃峰值水位
     * @param started     累计开始轮次
     * @param okFinished  累计正常完结轮次（含护栏拒绝以 reason 文本完结的轮）
     * @param failed      累计异常终结轮次
     */
    public record Snapshot(int active, int peakActive,
                           long started, long okFinished, long failed) {
    }
}

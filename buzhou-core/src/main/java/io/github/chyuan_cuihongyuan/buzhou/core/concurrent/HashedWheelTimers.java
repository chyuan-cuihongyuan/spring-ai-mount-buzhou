package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.List;

/**
 * 时间轮定时器（spec 3030 / T5061 / impl 2031）——hashed timing
 * wheel 思想（Netty HashedWheelTimer / Kafka pctimer）：定时任务
 * 按截止期哈希进环形槽——**O(1) 入轮**、到期只扫时针掠过的槽
 * （全量堆/有序表逐次比较病的根治）；大延迟自然多轮滞槽（每次
 * 时针掠过校验 deadline，未到留槽）。时间由调用方传入（确定性
 * 免注入时钟）；免线程（推进归调用方 tick 循环——纯数据结构件）。
 *
 * <p>守恒对账：scheduled == fired + pending；同槽同刻按 id 序
 * （入轮序——确定性）；粒度诚实边界：到期校验发生在**槽重访时**
 * ——deadline 过后最多再等一个轮周长（wheelSize×tick）内出（轮
 * 周长即最大额外延迟，口径显式可诺）；不做取消（handle 级生命
 * 周期留白）。
 */
public final class HashedWheelTimers {

    /** 定时任务（截止期毫秒 + 单调 id + 标识）。 */
    public record Timer(long deadlineMillis, long id, String label) {
    }

    private final int wheelSize;
    private final long tickMillis;
    private final List<List<Timer>> slots;
    private long currentTick;
    private long nextId;
    private long scheduledCount;
    private long firedCount;

    /** 槽数 ≥2 / tick ≥1ms（分辨率即 tick）。 */
    public HashedWheelTimers(int wheelSize, long tickMillis) {
        if (wheelSize < 2 || tickMillis < 1) {
            throw new IllegalArgumentException("wheelSize ≥ 2 且 tickMillis ≥ 1：" + wheelSize + "/" + tickMillis);
        }
        this.wheelSize = wheelSize;
        this.tickMillis = tickMillis;
        this.slots = new ArrayList<>(wheelSize);
        for (int i = 0; i < wheelSize; i++) {
            slots.add(new ArrayList<>());
        }
    }

    /**
     * 入轮（O(1)）：deadline = now + delay，落槽
     * (deadline/tick) % wheelSize——大延迟多轮滞槽自然成立。
     *
     * @return 单调任务 id（同刻 firing 序锚点）
     */
    public long schedule(long nowMillis, long delayMillis, String label) {
        if (delayMillis < 0) {
            throw new IllegalArgumentException("delay ≥ 0：" + delayMillis);
        }
        long deadline = nowMillis + delayMillis;
        long id = nextId++;
        int slot = (int) ((deadline / tickMillis) % wheelSize);
        slots.get(slot).add(new Timer(deadline, id, label));
        scheduledCount++;
        return id;
    }

    /**
     * 推进到 now：时针逐 tick 前进，掠过的槽内 deadline ≤ now 者
     * 出轮（未到者留槽下一轮）；返回本轮到期集（按 deadline、
     * 同刻按 id 序——确定性）。
     */
    public List<Timer> advanceTo(long nowMillis) {
        long targetTick = nowMillis / tickMillis;
        List<Timer> fired = new ArrayList<>();
        while (currentTick < targetTick) {
            currentTick++;
            int slot = (int) (currentTick % wheelSize);
            collectDue(slots.get(slot), nowMillis, fired);
        }
        fired.sort((a, b) -> a.deadlineMillis() != b.deadlineMillis()
                ? Long.compare(a.deadlineMillis(), b.deadlineMillis())
                : Long.compare(a.id(), b.id()));
        firedCount += fired.size();
        return fired;
    }

    /** 在轮未火任务数。 */
    public int pending() {
        int total = 0;
        for (List<Timer> slot : slots) {
            total += slot.size();
        }
        return total;
    }

    /** 累计入轮数（守恒对账面）。 */
    public long scheduledCount() {
        return scheduledCount;
    }

    /** 累计到期数（守恒对账面）。 */
    public long firedCount() {
        return firedCount;
    }

    private static void collectDue(List<Timer> slot, long nowMillis, List<Timer> fired) {
        slot.removeIf(timer -> {
            boolean due = timer.deadlineMillis() <= nowMillis;
            if (due) {
                fired.add(timer);
            }
            return due;
        });
    }
}

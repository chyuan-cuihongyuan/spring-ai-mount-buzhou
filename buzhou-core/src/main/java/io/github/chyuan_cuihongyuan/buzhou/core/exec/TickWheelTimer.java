package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 刻度轮定时器（spec 2042 / T3185 / impl 1593）——Netty hashed wheel
 * timer 思想：海量定时任务的 O(1) 调度——任务按延迟散进轮槽，超一轮
 * 跨度的任务挂剩余圈数（round），tick 推进只查当前槽（圈数尽的任务
 * 到期）。纯逻辑无线程：tick 由调用方驱动（确定性可回放）。
 *
 * <p>synchronized 小临界区；任务 id 幂等（重复 schedule 旧失效）。
 */
public final class TickWheelTimer {

    /** 默认轮槽 数（2 的幂——位与取模）。 */
    public static final int DEFAULT_WHEEL_SIZE = 64;

    private record Scheduled(String taskId, int remainingRounds) {
    }

    private final int wheelSize;
    private final int mask;
    private final List<List<Scheduled>> wheel;
    private final Map<String, Integer> taskSlots = new HashMap<>();
    private int cursor;
    private long ticks;

    /** 契约：wheelSize 为 ≥8 的 2 的幂（fail-fast）。 */
    public TickWheelTimer(int wheelSize) {
        if (wheelSize < 8 || Integer.bitCount(wheelSize) != 1) {
            throw new IllegalArgumentException("wheelSize 须为 ≥8 的 2 的幂：" + wheelSize);
        }
        this.wheelSize = wheelSize;
        this.mask = wheelSize - 1;
        this.wheel = new ArrayList<>(wheelSize);
        for (int i = 0; i < wheelSize; i++) {
            wheel.add(new ArrayList<>());
        }
    }

    public TickWheelTimer() {
        this(DEFAULT_WHEEL_SIZE);
    }

    /**
     * 调度：delayTicks 后到期（slot = (cursor+delay) mod size，跨轮挂
     * 圈数）。重复 id 旧任务失效（幂等）。契约：taskId 非空、delay ≥ 1
     *（0 延迟无意义——立即执行归调用方）。
     */
    public synchronized void schedule(String taskId, int delayTicks) {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId 不能为空");
        }
        if (delayTicks < 1) {
            throw new IllegalArgumentException("delayTicks 须 ≥ 1：" + delayTicks);
        }
        cancel(taskId); // 幂等：旧任务失效
        int slot = (cursor + delayTicks) & mask;
        int rounds = (delayTicks - 1) / wheelSize; // 圈数——首轮访问即到期者 0（跨 (delay−1)/W 整轮）
        wheel.get(slot).add(new Scheduled(taskId, rounds));
        taskSlots.put(taskId, slot);
    }

    /**
     * 推进一 tick：返回本 tick 到期的任务（当前槽圈数尽者）；未尽的
     * 圈数 −1 留槽。
     */
    public synchronized List<String> advance() {
        cursor = (cursor + 1) & mask;
        ticks++;
        List<Scheduled> slotTasks = wheel.get(cursor);
        List<String> due = new ArrayList<>();
        List<Scheduled> remaining = new ArrayList<>();
        for (Scheduled s : slotTasks) {
            if (s.remainingRounds() == 0) {
                due.add(s.taskId());
                taskSlots.remove(s.taskId());
            } else {
                remaining.add(new Scheduled(s.taskId(), s.remainingRounds() - 1));
            }
        }
        if (!remaining.isEmpty() || !due.isEmpty()) {
            wheel.set(cursor, remaining); // 无条件写回——rounds 递减不因 due 空而丢
        }
        return due;
    }

    /** 取消任务（未调度 false）。 */
    public synchronized boolean cancel(String taskId) {
        if (taskId == null) {
            throw new IllegalArgumentException("taskId 不能为 null");
        }
        Integer slot = taskSlots.remove(taskId);
        if (slot == null) {
            return false;
        }
        wheel.get(slot).removeIf(s -> s.taskId().equals(taskId));
        return true;
    }

    /** 在调度任务数（积压面）。 */
    public synchronized int pendingCount() {
        return taskSlots.size();
    }

    /** 已推进 tick 数。 */
    public synchronized long ticks() {
        return ticks;
    }
}

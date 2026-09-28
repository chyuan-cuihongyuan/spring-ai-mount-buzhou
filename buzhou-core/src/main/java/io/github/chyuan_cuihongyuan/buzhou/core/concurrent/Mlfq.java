package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * MLFQ 多级反馈队列（spec 6028 / T6251 续 / impl 2229）——
 * OS 调度经典 MLFQ 思想：**多级队列+量子递增+用满降级**——
 * 新任务进最高级（交互优先），量子用满未完成则降一级
 * （量子随级翻倍——CPU 密集型逐渐沉底），同级 FIFO 轮转
 * ——交互任务被 CPU 密集任务压死（无优先级区分）与固定
 * 优先级饿死低级（无反馈降级）的病解。协作式模型（单运行
 * 槽：dispatch → onQuantumExpired/onCompleted 二选一）。
 * 确定性定构（同提交序同调度序）。
 *
 * <p>与 EdfScheduler（concurrent）同族不同面：截止时间优先
 * vs 行为反馈降级；与 AgingPriorityQueue（exec）不同面：
 * 时间片老化 vs 优先级老化。
 */
public final class Mlfq {

    /** 派发结果（任务 id、所在级、本级量子）。 */
    public record Dispatch(long taskId, int level, long quantum) {
    }

    private final Deque<Long>[] levels;
    private final long baseQuantum;
    private final Map<Long, Integer> activeLevelOf = new HashMap<>();
    private Dispatch running;

    /** levelCount 级、0 级量子 baseQuantum、每降级翻倍
     *  （levelCount∈[1,16]、baseQuantum≥1 fail-fast）。 */
    @SuppressWarnings("unchecked")
    public Mlfq(int levelCount, long baseQuantum) {
        if (levelCount < 1 || levelCount > 16) {
            throw new IllegalArgumentException("级数须在 [1,16]: " + levelCount);
        }
        if (baseQuantum < 1) {
            throw new IllegalArgumentException("基础量子必须为正: " + baseQuantum);
        }
        this.levels = new Deque[levelCount];
        for (int i = 0; i < levelCount; i++) {
            this.levels[i] = new ArrayDeque<>();
        }
        this.baseQuantum = baseQuantum;
    }

    /** 提交任务（进 0 级队尾；重复活动任务 fail-fast）。 */
    public void submit(long taskId) {
        if (activeLevelOf.containsKey(taskId)) {
            throw new IllegalArgumentException("任务已活动: " + taskId);
        }
        levels[0].addLast(taskId);
        activeLevelOf.put(taskId, 0);
    }

    /** 派发最高级队首（有任务在运行 fail-fast；空返回 null）。 */
    public Dispatch dispatch() {
        if (running != null) {
            throw new IllegalArgumentException("有任务在运行须先回报: " + running.taskId());
        }
        for (int level = 0; level < levels.length; level++) {
            Deque<Long> queue = levels[level];
            if (!queue.isEmpty()) {
                long taskId = queue.pollFirst();
                long quantum = baseQuantum << level;
                running = new Dispatch(taskId, level, quantum);
                return running;
            }
        }
        return null;
    }

    /** 量子用满未完成（降一级或沉底级；无运行任务 fail-fast）。 */
    public void onQuantumExpired(long taskId) {
        requireRunning(taskId);
        int current = running.level();
        int next = Math.min(current + 1, levels.length - 1);
        running = null;
        activeLevelOf.put(taskId, next);
        levels[next].addLast(taskId);
    }

    /** 任务完成（移出系统；无运行任务 fail-fast）。 */
    public void onCompleted(long taskId) {
        requireRunning(taskId);
        running = null;
        activeLevelOf.remove(taskId);
    }

    /** 活动任务数读数（含运行中）。 */
    public int activeTasks() {
        return activeLevelOf.size();
    }

    /** 指定级就绪队列长读数（审计用）。 */
    public int readyAt(int level) {
        if (level < 0 || level >= levels.length) {
            throw new IllegalArgumentException("级越界: " + level);
        }
        return levels[level].size();
    }

    private void requireRunning(long taskId) {
        if (running == null || running.taskId() != taskId) {
            throw new IllegalArgumentException("任务不在运行: " + taskId);
        }
    }
}

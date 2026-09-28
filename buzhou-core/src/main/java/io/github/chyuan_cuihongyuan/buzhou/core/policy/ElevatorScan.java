package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.TreeSet;

/**
 * Elevator Scan 电梯扫掠（spec 6033 / T6265 / impl 2234）——
 * 磁盘调度 SCAN/LOOK 思想：**沿当前方向服务至该向最远请求
 * 再折返**（LOOK：不到物理端，到最远请求即折），同向按距离
 * 升序服务——FIFO 调度往返空跑（寻道放大）与 SSTF 近者插队
 * 饿死远端（无全局序）的病解。显式磁头位+方向（服务后磁头
 * 停在末次服务轨道、方向翻转——状态可审计）；TreeSet 双向
 * 视图（同请求集同头位同方向同序——确定性）。重复请求幂等。
 *
 * <p>与 DeficitRoundRobin（5027）同族不同面：寻道路径扫掠
 * vs 公平份额记账。静态快照面（serveAll 一次出全序）。
 */
public final class ElevatorScan {

    /** 移动方向。 */
    public enum Direction {
        UP, DOWN
    }

    private final TreeSet<Long> pending = new TreeSet<>();
    private final long maxTrack;
    private Direction direction;
    private long head;

    /** 建调度器（磁头位/轨道域/方向；越域或倒置 fail-fast）。 */
    public ElevatorScan(long head, long maxTrack, Direction direction) {
        if (maxTrack < 0) {
            throw new IllegalArgumentException("maxTrack 必须非负: " + maxTrack);
        }
        if (head < 0 || head > maxTrack) {
            throw new IllegalArgumentException("磁头越域 [0," + maxTrack + "]: " + head);
        }
        if (direction == null) {
            throw new IllegalArgumentException("方向非空");
        }
        this.head = head;
        this.maxTrack = maxTrack;
        this.direction = direction;
    }

    /** 请求入队（越域 fail-fast；重复幂等）。 */
    public void add(long track) {
        if (track < 0 || track > maxTrack) {
            throw new IllegalArgumentException("轨道越域 [0," + maxTrack + "]: " + track);
        }
        pending.add(track);
    }

    /** 扫掠服务全请求（LOOK 折返；返回服务轨道序）。 */
    public Deque<Long> serveAll() {
        Deque<Long> order = new ArrayDeque<>();
        if (direction == Direction.UP) {
            for (long t : pending.tailSet(head, true)) {
                order.addLast(t);
            }
            for (long t : pending.headSet(head, false).descendingSet()) {
                order.addLast(t);
            }
        } else {
            for (long t : pending.headSet(head, true).descendingSet()) {
                order.addLast(t);
            }
            for (long t : pending.tailSet(head, false)) {
                order.addLast(t);
            }
        }
        if (!order.isEmpty()) {
            head = order.getLast();
        }
        pending.clear();
        direction = direction == Direction.UP ? Direction.DOWN : Direction.UP;
        return order;
    }

    /** 待服务数读数。 */
    public int pendingCount() {
        return pending.size();
    }

    /** 磁头位读数。 */
    public long head() {
        return head;
    }

    /** 当前方向读数。 */
    public Direction direction() {
        return direction;
    }
}

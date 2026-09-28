package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.concurrent.atomic.AtomicReference;

/**
 * MCS Lock 队列锁（spec 6043 / T6285 / impl 2243）——
 * Linux 内核 MCS 锁思想：**每等待者本地自旋**——等待者入队
 * 后自旋在自己的节点上（前驱仅在自己节点的 locked 位上写
 * 一次），无全局自旋变量缓存行争用——TicketLock 全局票号
 * 自旋（所有等待者挤同一缓存行）的病解。锁转移由前驱显式
 * 交接（队列 FIFO 公平）。
 *
 * <p>与 TicketLock（spec 5006）同族不同面：全局票号自旋 vs
 * 每节点本地自旋；与 SeqLock（5007）不同面：互斥写锁 vs
 * 乐观读序号。
 */
public final class McsLock {

    public static final class Node {
        volatile boolean locked;
        volatile Node next;
    }

    private final AtomicReference<Node> tail = new AtomicReference<>();

    /** 入队并自旋等待（返回节点句柄供 unlock 使用）。 */
    public Node lock() {
        Node me = new Node();
        Node predecessor = tail.getAndSet(me);
        if (predecessor != null) {
            me.locked = true;
            predecessor.next = me;
            while (me.locked) {
                Thread.onSpinWait();
            }
        }
        return me;
    }

    /** 解锁（交接给后继或摘除自身；null/非持有者 fail-fast）。 */
    public void unlock(Node me) {
        if (me == null) {
            throw new IllegalArgumentException("节点非空");
        }
        if (me.next == null) {
            if (tail.compareAndSet(me, null)) {
                return;
            }
            while (me.next == null) {
                Thread.onSpinWait();
            }
        }
        me.next.locked = false;
    }
}

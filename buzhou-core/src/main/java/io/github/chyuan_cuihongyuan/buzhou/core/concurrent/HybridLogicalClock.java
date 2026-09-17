package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.function.LongSupplier;

/**
 * 混合逻辑时钟（spec 3007 / T5015 / impl 2008）——HLC 思想
 * （Kulkarni 2014 / CockroachDB 实践）：墙钟为骨、计数器为髓——
 * (wall, counter) 二元组：wall 贴物理时间（可比、可读、漂移有界），
 * counter 在同 wall 内单调递增保因果序。**单调不倒**（物理钟回拨
 * /对端超前都吸收——max 三路合并）；因果保持（happens-before ⇒
 * 时间戳严格小于），但反向不成立（同 wall 并发可分序——与向量
 * 时钟的真并发判别互补）。
 *
 * <p>物理钟 LongSupplier 注入（测试确定性 / 生产 System::millis）；
 * 非线程安全（单事件线程口径）。
 */
public final class HybridLogicalClock {

    /** 未初始化墙：首事件（tick/observe）才落物理基线（pt,0）。 */
    private static final long UNINITIALIZED_WALL = Long.MIN_VALUE;

    /** HLC 时间戳（墙钟毫秒 + 同墙计数——墙先比、计数后比）。 */
    public record Hlc(long wall, int counter) implements Comparable<Hlc> {
        @Override
        public int compareTo(Hlc other) {
            int byWall = Long.compare(wall, other.wall);
            return byWall != 0 ? byWall : Integer.compare(counter, other.counter);
        }
    }

    private final LongSupplier physicalClock;
    private long wall;
    private int counter;

    /** 生产构造（物理钟 = System.currentTimeMillis）。 */
    public HybridLogicalClock() {
        this(System::currentTimeMillis);
    }

    /** 注入物理钟（确定性测试/自定义时基；首事件才建基线）。 */
    public HybridLogicalClock(LongSupplier physicalClock) {
        this.physicalClock = physicalClock;
        this.wall = UNINITIALIZED_WALL;
        this.counter = 0;
    }

    /**
     * 本地事件：wall' = max(wall, pt)；同墙计数 +1，墙前进则清零
     * ——单调严格递增（同墙靠计数、跳墙天然大）。
     */
    public Hlc tick() {
        long now = physicalClock.getAsLong();
        if (now > wall) {
            wall = now;
            counter = 0;
        } else {
            counter++;
        }
        return new Hlc(wall, counter);
    }

    /**
     * 接收远端事件（因果合并）：wall' = max(wall, pt, remote.wall)；
     * 三路同墙取 max(counter)+1；两路同墙取在侧 +1；全新墙清零
     * ——远端超前吸收为本地新基线，远端落后无感。
     */
    public Hlc observe(Hlc remote) {
        long now = physicalClock.getAsLong();
        long merged = Math.max(wall, Math.max(now, remote.wall()));
        if (merged == wall && merged == remote.wall()) {
            counter = Math.max(counter, remote.counter()) + 1;
        } else if (merged == wall) {
            counter++;
        } else if (merged == remote.wall()) {
            counter = remote.counter() + 1;
        } else {
            counter = 0;
        }
        wall = merged;
        return new Hlc(wall, counter);
    }

    /** 当前时间戳只读（不发事件、不动账面）。 */
    public Hlc peek() {
        return new Hlc(wall, counter);
    }
}

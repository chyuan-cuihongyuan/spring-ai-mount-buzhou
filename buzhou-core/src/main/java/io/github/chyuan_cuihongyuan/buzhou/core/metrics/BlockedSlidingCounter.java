package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Blocked Sliding Counter 分块滑窗计数器（spec 5040 / T6181 /
 * impl 2191）——Datar-Gionis 指数直方图同族的分块近似面
 * （√N 分块滑窗思想）：把窗口切成 m 个块（块长 B=⌈N/m⌉），
 * 每块精确计数、整块进整块出——真值恒落在
 * **[Σ块计数−最旧块计数, Σ块计数]**（最旧块只知部分在窗——
 * 误差 ≤ 一个块计数 ≤ B），内存 O(m) 而非 O(N)——每事件
 * 一个计数的精确滑窗（内存随窗口线性爆）与无界衰减计数
 * （窗口界不可知）的病解。确定性无时间依赖（位置由调用方
 * 事件驱动）。
 *
 * <p>与 SlidingWindowCounter（ratelimit 限流滑窗）同族不同面：
 * 固定单位桶限流计数 vs 任意事件流近似计数+显式误差界。
 */
public final class BlockedSlidingCounter {

    private static final class Block {
        long endPosition;
        long hits;

        Block(long endPosition) {
            this.endPosition = endPosition;
        }
    }

    private final long windowSize;
    private final long blockSize;
    private final Deque<Block> blocks = new ArrayDeque<>();
    private long position;
    private long totalHitsInBlocks;

    /** 定构（windowSize≥1、blockCount≥1；块长=⌈windowSize/blockCount⌉）。 */
    public BlockedSlidingCounter(long windowSize, long blockCount) {
        if (windowSize < 1) {
            throw new IllegalArgumentException("windowSize≥1：" + windowSize);
        }
        if (blockCount < 1) {
            throw new IllegalArgumentException("blockCount≥1：" + blockCount);
        }
        this.windowSize = windowSize;
        this.blockSize = (windowSize + blockCount - 1) / blockCount;
        blocks.addLast(new Block(this.blockSize));
    }

    /** 事件推进一格（hit=真记一次命中；块整块滚动进出）。 */
    public void add(boolean hit) {
        position++;
        Block current = blocks.peekLast();
        if (position > current.endPosition) {
            current = rollIntoNewBlock();
        }
        if (hit) {
            current.hits++;
            totalHitsInBlocks++;
        }
        expireFullyOutBlocks();
    }

    /**
     * 近似计数：下界=Σ块−最旧块（最旧块整块扣除）；
     * 上界见 {@link #upperBound()}——真值恒在两界之间。
     */
    public long lowerEstimate() {
        return Math.max(0, totalHitsInBlocks - oldestHits());
    }

    /** 计数上界（Σ块计数——最旧块整块计入）。 */
    public long upperBound() {
        return totalHitsInBlocks;
    }

    /** 当前事件位读数。 */
    public long position() {
        return position;
    }

    /** 在册块数读数（≤块配额+1）。 */
    public int blockCount() {
        return blocks.size();
    }

    /** 块长读数。 */
    public long blockSize() {
        return blockSize;
    }

    private Block rollIntoNewBlock() {
        Block next = new Block(position / blockSize * blockSize + blockSize);
        blocks.addLast(next);
        return next;
    }

    private void expireFullyOutBlocks() {
        long fullyOutCutoff = position - windowSize;
        while (blocks.size() > 1) {
            Block oldest = blocks.peekFirst();
            if (oldest.endPosition > fullyOutCutoff) {
                break;
            }
            blocks.pollFirst();
            totalHitsInBlocks -= oldest.hits;
        }
    }

    private long oldestHits() {
        Iterator<Block> iterator = blocks.iterator();
        return iterator.next().hits;
    }
}

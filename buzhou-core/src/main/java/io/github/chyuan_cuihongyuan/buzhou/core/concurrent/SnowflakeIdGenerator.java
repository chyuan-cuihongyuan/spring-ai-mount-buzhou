package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.Random;

/**
 * Snowflake 发号器（spec 7036 / U7273 / impl 2288）——Twitter
 * Snowflake 思想（分布式 ID 事实标准）：**64 位 = 1 符号 +
 * 41 时间毫秒 + 10 机器 + 12 序列**——同毫秒内序列自增、
 * 跨毫秒重置；时钟回退 fail-fast（回拨期间拒绝发号——
 * 静默回拨发重号的病根排除）；序列溢出自旋至下毫秒（此
 * 处以注入时钟显式推进——零真实睡眠完全确定）。与
 * UuidV7Monotonic（同包）同族不同面：时间有序 UUID vs
 * 分段整数发号（时钟回拨守卫面）。ids 从注入时钟读取
 * ——零真实时钟依赖。
 */
public final class SnowflakeIdGenerator {

    private static final long EPOCH_OFFSET = 1704067200000L;
    private static final int MACHINE_BITS = 10;
    private static final int SEQUENCE_BITS = 12;
    private static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;

    private final int machineId;
    private final java.util.function.LongSupplier clock;
    private long lastTimestamp = -1L;
    private long sequence;

    /** machineId∈[0,1024)；时钟注入（越域 fail-fast）。 */
    public SnowflakeIdGenerator(int machineId, java.util.function.LongSupplier clock) {
        if (machineId < 0 || machineId >= (1 << MACHINE_BITS)) {
            throw new IllegalArgumentException("机器号越域 [0,1024): " + machineId);
        }
        if (clock == null) {
            throw new IllegalArgumentException("时钟非空");
        }
        this.machineId = machineId;
        this.clock = clock;
    }

    /** 种子化便捷构造（兼容面——机器号+固定种子时钟步进 1ms）。 */
    public SnowflakeIdGenerator(int machineId, long seed) {
        this(machineId, new RandomDrivenClock(seed));
    }

    /** 发号（时钟回退 fail-fast；同毫秒 4096 个上限——自旋下毫秒）。 */
    public synchronized long nextId() {
        long now = clock.getAsLong();
        if (now < lastTimestamp) {
            throw new IllegalStateException("时钟回退 " + (lastTimestamp - now) + "ms——拒绝发号");
        }
        if (now == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                while (now <= lastTimestamp) {
                    now = clock.getAsLong();
                }
            }
        } else {
            sequence = 0;
        }
        lastTimestamp = now;
        return ((now - EPOCH_OFFSET) << (MACHINE_BITS + SEQUENCE_BITS))
                | ((long) machineId << SEQUENCE_BITS)
                | sequence;
    }

    /** 注入步进时钟（1ms/次——确定性测试面）。 */
    private static final class RandomDrivenClock implements java.util.function.LongSupplier {
        private long current;

        RandomDrivenClock(long seed) {
            this.current = EPOCH_OFFSET + new Random(seed).nextInt(1000);
        }

        @Override
        public long getAsLong() {
            long value = current;
            current += 1;
            return value;
        }
    }

    /** 分解读数（id → [时间戳偏移,机器号,序列]）。 */
    public static long[] decompose(long id) {
        return new long[]{
                (id >> (MACHINE_BITS + SEQUENCE_BITS)) + EPOCH_OFFSET,
                (id >> SEQUENCE_BITS) & ((1 << MACHINE_BITS) - 1),
                id & SEQUENCE_MASK};
    }
}

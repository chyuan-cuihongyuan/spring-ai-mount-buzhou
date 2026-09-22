package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

/**
 * 雪花 ID 分解（spec 1908 / T3017 / impl 1509）——Twitter Snowflake
 * 64 位布局：1 符号位（恒 0）+ 41 位毫秒时间戳（相对自定义纪元）+
 * 10 位机器号 + 12 位同毫秒序列。ID 自描述：分解即得生成时刻、
 * 来源实例与并发序——排障不查库。
 *
 * <p>纯位运算零状态；不做生成与时钟回拨处置（归生成器）。
 */
public final class SnowflakeIdDecompose {

    /** 布局常量：10 位机器号（1024 实例）、12 位序列（4096/毫秒）。 */
    public static final long WORKER_BITS = 10;
    public static final long SEQUENCE_BITS = 12;
    /** 机器号掩码 0x3FF、序列掩码 0xFFF。 */
    public static final long WORKER_MASK = (1L << WORKER_BITS) - 1;
    public static final long SEQUENCE_MASK = (1L << SEQUENCE_BITS) - 1;
    /** 时间戳左移位 = 机器位 + 序列位 = 22。 */
    public static final int TIMESTAMP_SHIFT = (int) (WORKER_BITS + SEQUENCE_BITS);

    /** 分解结果：生成时刻（绝对毫秒）、机器号、同毫秒序列。 */
    public record Decomposed(long timestampMillis, long workerId, long sequence) {
    }

    /**
     * 按位拆解：时间戳 = (id >>> 22) + epoch；机器号 = (id >>> 12)
     * & 0x3FF；序列 = id & 0xFFF。契约：id ≥ 0（符号位恒 0，
     * fail-fast）。
     */
    public static Decomposed decompose(long id, long epochMillis) {
        if (id < 0) {
            throw new IllegalArgumentException("id 符号位必须为 0：" + id);
        }
        long timestamp = (id >>> TIMESTAMP_SHIFT) + epochMillis;
        long worker = (id >>> SEQUENCE_BITS) & WORKER_MASK;
        long sequence = id & SEQUENCE_MASK;
        return new Decomposed(timestamp, worker, sequence);
    }

    /**
     * 逆组装：compose(decompose(id)) == id。契约：timestampMillis ≥
     * epochMillis、worker ∈ [0,1023]、sequence ∈ [0,4095]
     * （fail-fast）。
     */
    public static long compose(long timestampMillis, long workerId,
                               long sequence, long epochMillis) {
        if (timestampMillis < epochMillis) {
            throw new IllegalArgumentException(
                    "时间戳早于纪元：" + timestampMillis + " < " + epochMillis);
        }
        if (workerId < 0 || workerId > WORKER_MASK) {
            throw new IllegalArgumentException(
                    "机器号须在 [0,1023]：" + workerId);
        }
        if (sequence < 0 || sequence > SEQUENCE_MASK) {
            throw new IllegalArgumentException(
                    "序列须在 [0,4095]：" + sequence);
        }
        return ((timestampMillis - epochMillis) << TIMESTAMP_SHIFT)
                | (workerId << SEQUENCE_BITS)
                | sequence;
    }
}

package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.Random;
import java.util.UUID;
import java.util.function.LongSupplier;

/**
 * UUIDv7 时间有序生成器（spec 4010 / T6021 / impl 2111）——RFC 9562
 * 思想：48 位 Unix 毫秒时间戳置最高位——**字典序即时间序**（索引
 * 局部性、范围扫友好，v4 均匀随机把 B 树插成随机写病的根治）；
 * rand_a 落 12 位**单调计数器**（同毫秒内递增——同键分页/批量生成
 * 不撞序）；溢出向时间戳**借位**（unix_ts_ms+1——RFC 允许的伪时序
 * 推进，不打断有序性）；rand_b 62 位随机 + variant 10。
 *
 * <p>时钟与随机源可注入（确定性回放面）。与 SnowflakeIdDecompose
 * （协调位布局）成对：v7 零协调、以时间戳换排序性。
 */
public final class UuidV7Monotonic {

    private static final int COUNTER_MAX = 0xFFF;   // rand_a 12 位

    private final LongSupplier clockMillis;
    private final Random random;
    private long lastTs = -1;
    private int counter = -1;

    /** 定构（时钟/随机源非 null——注入确定性源可回放）。 */
    public UuidV7Monotonic(LongSupplier clockMillis, Random random) {
        if (clockMillis == null || random == null) {
            throw new IllegalArgumentException("clockMillis/random 非空");
        }
        this.clockMillis = clockMillis;
        this.random = random;
    }

    /** 默认源定构（系统钟 + 通用随机）。 */
    public UuidV7Monotonic() {
        this(System::currentTimeMillis, new Random());
    }

    /** 生成下一个 v7：新毫秒计数器随机重置、同毫秒 +1、溢出向时间戳借位。 */
    public synchronized UUID next() {
        long now = clockMillis.getAsLong();
        if (now != lastTs) {
            lastTs = now;
            counter = random.nextInt(COUNTER_MAX + 1);
        } else if (counter == COUNTER_MAX) {
            lastTs = ++now;   // 借位：伪时序推进 1ms，有序性不断
            counter = 0;
        } else {
            counter++;
        }
        long msb = (lastTs << 16) | (7L << 12) | counter;   // ver 7 + 12 位计数器
        long lsb = 0x8000_0000_0000_0000L | (random.nextLong() & 0x3FFF_FFFF_FFFF_FFFFL);   // variant 10
        return new UUID(msb, lsb);
    }

    /** v7 时间戳回读（48 位 Unix 毫秒——msb 高位）。 */
    public static long timestampOf(UUID id) {
        if (id == null || id.version() != 7) {
            throw new IllegalArgumentException("id 非 null 且 version=7");
        }
        return id.getMostSignificantBits() >>> 16;
    }

    /** 单调计数器回读（rand_a 12 位——同毫秒内序）。 */
    public static int counterOf(UUID id) {
        if (id == null || id.version() != 7) {
            throw new IllegalArgumentException("id 非 null 且 version=7");
        }
        return (int) (id.getMostSignificantBits() & COUNTER_MAX);
    }
}

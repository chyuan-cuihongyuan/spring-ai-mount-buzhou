package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import java.util.ArrayDeque;
import java.util.List;

/**
 * Redis 慢操作榜（spec 1418 / T2137 / impl 1071）——Redis SLOWLOG 思想
 * （严格大于阈值才入榜、有界 FIFO、读面即现场）应用于仓内 Redis 存储操作：
 * {@code redis-cli slowlog} 看服务端，本榜看<b>客户端视角</b>的往返耗时——
 * 网络抖动/大 key/慢查询在服务端慢日志里可能完全合法，客户端延迟才说真话。
 *
 * <p>与 J 会话 ToolSlowLog（core/exec，工具侧）同型扩散到 store-redis 侧：
 * 阈值静态可调（{@link #configureThresholdMillis}）、有界 FIFO
 * {@value #CAPACITY} 条、{@link #entries()} 新→旧现场快照；调用点
 * finally 计时（异常路径也入账——慢与败正交）。纯静态面：
 * {@link #resetForTest()} 归零注入点（进程级先例）。
 */
public final class RedisSlowOpLog {

    /** 榜容量（有界 FIFO，挤旧）。 */
    public static final int CAPACITY = 32;

    /** 默认慢阈值（毫秒；Redis 客户端视角 >100ms 已属异常）。 */
    public static final long DEFAULT_THRESHOLD_MILLIS = 100;

    private static final ArrayDeque<Entry> LOG = new ArrayDeque<>(CAPACITY);
    private static volatile long thresholdMillis = DEFAULT_THRESHOLD_MILLIS;
    private static final Object LOCK = new Object();
    private static long totalSlow;

    /** 榜内一条现场：操作名 + 客户端往返耗时。 */
    public record Entry(String op, long durationMillis) {
    }

    private RedisSlowOpLog() {
    }

    /** 调用点缝：耗时严格大于阈值才入榜（不达阈值仅一次 volatile 比较）。 */
    public static void record(String op, long durationMillis) {
        if (durationMillis <= thresholdMillis) {
            return;
        }
        synchronized (LOCK) {
            totalSlow++;
            if (LOG.size() >= CAPACITY) {
                LOG.pollFirst(); // 挤掉最旧
            }
            LOG.addLast(new Entry(op, durationMillis));
        }
    }

    /** 读面：当前榜内现场（新→旧；防御拷贝）。 */
    public static List<Entry> entries() {
        synchronized (LOCK) {
            var it = LOG.descendingIterator();
            List<Entry> newestFirst = new java.util.ArrayList<>(LOG.size());
            while (it.hasNext()) {
                newestFirst.add(it.next());
            }
            return List.copyOf(newestFirst);
        }
    }

    /** 累计慢操作次数（含已被挤出榜的；水位语义）。 */
    public static long totalSlowOps() {
        synchronized (LOCK) {
            return totalSlow;
        }
    }

    /** 阈值调整（运行时可调；返回旧值便于测试恢复）。 */
    public static long configureThresholdMillis(long newThresholdMillis) {
        long old = thresholdMillis;
        thresholdMillis = newThresholdMillis;
        return old;
    }

    /** 测试归零口：榜/累计清空 + 阈值复位。 */
    public static void resetForTest() {
        synchronized (LOCK) {
            LOG.clear();
            totalSlow = 0;
        }
        thresholdMillis = DEFAULT_THRESHOLD_MILLIS;
    }
}

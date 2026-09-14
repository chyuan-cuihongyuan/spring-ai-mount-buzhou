package io.github.chyuan_cuihongyuan.buzhou.core.eval;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 评估运行年龄台账（spec 1420 / T2141 / impl 1073）——tqdm 进度可见性 +
 * k8s「Pod 运行 3 天」运行时长异味思想：{@link EvalRunRegistry} 的 active
 * 计数不含年龄——「最老的活跃 run 开了两小时」这类卡死异味混在正常长跑里。
 * 进程级静态读面（ToolArgsValidator.validationStats 同款先例）：
 * Registry.Registration begin/close 双点埋点（只增记账），
 * {@link #resetForTest()} 归零注入点。
 *
 * <p>快照口径：{@code oldestActiveAgeMillis} 以最早仍开启的运行为准（卡死
 * 哨兵；无活跃 = -1）；{@code maxCompletedDurationMillis} 为历史最长完成
 * 时长水位（挤出也累计语义——单调不回退）。
 */
public final class EvalRunAgeLedger {

    private static final ConcurrentHashMap<Long, Long> OPENED_AT = new ConcurrentHashMap<>();
    private static final AtomicLong OPENED = new AtomicLong();
    private static final AtomicLong CLOSED = new AtomicLong();
    private static final AtomicLong MAX_COMPLETED_MILLIS = new AtomicLong();

    private EvalRunAgeLedger() {
    }

    /** Registry 埋点：运行开启（registrationId → 开启时刻）。 */
    public static void recordOpened(long registrationId) {
        OPENED_AT.put(registrationId, System.currentTimeMillis());
        OPENED.incrementAndGet();
    }

    /** Registry 埋点：运行终结（最长完成时长水位单调更新）。 */
    public static void recordClosed(long registrationId) {
        Long openedAt = OPENED_AT.remove(registrationId);
        if (openedAt == null) {
            return;
        }
        long duration = Math.max(0, System.currentTimeMillis() - openedAt);
        CLOSED.incrementAndGet();
        MAX_COMPLETED_MILLIS.accumulateAndGet(duration, Math::max);
    }

    /** 只读快照：活跃数/最老活跃年龄/最长完成水位/完成累计。 */
    public static Snapshot stats() {
        int active = OPENED_AT.size();
        long oldest = -1;
        long now = System.currentTimeMillis();
        for (Long openedAt : OPENED_AT.values()) {
            long age = Math.max(0, now - openedAt);
            oldest = oldest == -1 ? age : Math.max(oldest, age);
        }
        return new Snapshot(active, oldest, MAX_COMPLETED_MILLIS.get(), CLOSED.get());
    }

    /** 测试归零口：静态读数的 reset 注入点。 */
    public static void resetForTest() {
        OPENED_AT.clear();
        OPENED.set(0);
        CLOSED.set(0);
        MAX_COMPLETED_MILLIS.set(0);
    }

    /**
     * @param active                    当前活跃运行数
     * @param oldestActiveAgeMillis     最老活跃运行年龄（毫秒；无活跃 = -1 哨兵）
     * @param maxCompletedDurationMillis 历史最长完成时长（毫秒水位；无完成 = 0）
     * @param closed                    累计完成运行数
     */
    public record Snapshot(int active, long oldestActiveAgeMillis,
                           long maxCompletedDurationMillis, long closed) {
    }
}

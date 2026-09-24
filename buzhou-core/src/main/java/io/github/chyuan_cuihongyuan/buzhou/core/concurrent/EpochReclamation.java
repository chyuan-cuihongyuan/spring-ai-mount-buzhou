package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Epoch-Based Reclamation 时代回收（spec 5036 / T6173 /
 * impl 2187）——folly/libcds EBR 思想（Fraser 2004）：读者
 * `enter` 钉住当前全局时代（守卫），写者 `retire` 延迟回收
 * 项（记录退休时代）；全局时代推进后，某退休项只有在
 * **时代严格大于它且该时代及更早时代守卫清零**时才可回收
 * ——不可能释放仍被读者引用的节点（引用-释放竞争病的
 * 根治：无锁结构节点不能读完就 free）。确定性无时间依赖
 * （时代由调用方显式推进）。
 *
 * <p>与 SeqLock（spec 5007）同族不同面：奇偶序号乐观读
 * 一致 vs 延迟回收生命周期；与 TicketLock（spec 5006）
 * 不同面：公平自旋 vs 安全回收。
 */
public final class EpochReclamation {

    private static final class RetiredItem {
        final String itemId;
        final long epoch;

        RetiredItem(String itemId, long epoch) {
            this.itemId = itemId;
            this.epoch = epoch;
        }
    }

    /** 读者守卫（AutoCloseable——try-with-resources 即退）。 */
    public final class Guard implements AutoCloseable {
        private final long pinnedEpoch;
        private boolean closed;

        private Guard(long pinnedEpoch) {
            this.pinnedEpoch = pinnedEpoch;
            guardCounts.merge(pinnedEpoch, 1L, Long::sum);
            activeGuards++;
        }

        /** 钉住的时代读数。 */
        public long epoch() {
            requireOpen();
            return pinnedEpoch;
        }

        @Override
        public void close() {
            requireOpen();
            closed = true;
            guardCounts.merge(pinnedEpoch, -1L, Long::sum);
            if (guardCounts.get(pinnedEpoch) == 0) {
                guardCounts.remove(pinnedEpoch);
            }
            activeGuards--;
        }

        private void requireOpen() {
            if (closed) {
                throw new IllegalStateException("守卫已关闭");
            }
        }
    }

    private long currentEpoch;
    private int activeGuards;
    private final Map<Long, Long> guardCounts = new HashMap<>();
    private final Deque<RetiredItem> retired = new ArrayDeque<>();

    /** 读者进入：钉住当前时代（计数+1）。 */
    public Guard enter() {
        return new Guard(currentEpoch);
    }

    /** 写者推进全局时代（旧时代守卫继续钉住旧值直至退出）。 */
    public long advanceEpoch() {
        return ++currentEpoch;
    }

    /** 退休一项（记录当前时代；重复 id 未回收即 fail-fast）。 */
    public void retire(String itemId) {
        if (itemId == null || itemId.isEmpty()) {
            throw new IllegalArgumentException("itemId 非空");
        }
        for (RetiredItem item : retired) {
            if (item.itemId.equals(itemId)) {
                throw new IllegalArgumentException("退休项未回收不可重复：" + itemId);
            }
        }
        retired.addLast(new RetiredItem(itemId, currentEpoch));
    }

    /**
     * 回收当前可安全的退休项：时代 < 全局时代且该时代及
     * 更早时代守卫清零；返回按时代升序+退休序（确定性）。
     */
    public List<String> tryReclaim() {
        List<String> reclaimable = new ArrayList<>();
        for (RetiredItem item : retired) {
            if (item.epoch < currentEpoch && !hasGuardAtOrBefore(item.epoch)) {
                reclaimable.add(item.itemId);
            }
        }
        if (reclaimable.isEmpty()) {
            return List.of();
        }
        retired.removeIf(item -> reclaimable.contains(item.itemId));
        return List.copyOf(reclaimable);
    }

    /** 当前时代读数。 */
    public long currentEpoch() {
        return currentEpoch;
    }

    /** 活跃守卫数读数。 */
    public int activeGuards() {
        return activeGuards;
    }

    /** 未回收退休项数读数。 */
    public int retiredCount() {
        return retired.size();
    }

    private boolean hasGuardAtOrBefore(long epoch) {
        for (Map.Entry<Long, Long> entry : guardCounts.entrySet()) {
            if (entry.getKey() <= epoch && entry.getValue() > 0) {
                return true;
            }
        }
        return false;
    }
}

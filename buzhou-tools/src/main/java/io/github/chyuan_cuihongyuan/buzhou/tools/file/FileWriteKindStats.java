package io.github.chyuan_cuihongyuan.buzhou.tools.file;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 文件写型分类读面（L 会话 1700 系 R31 = effort #1730 / spec 1730 /
 * 票 T2661 + T2662 / impl 1330）——restic 备份变更分类思想（新增/未变/
 * 已变三分）：WriteFileTool 的写入是新建还是覆盖、覆盖有没有实际变化
 * ——变更分类显形写入画像，「全是不变覆盖」= 模型在空转写。
 *
 * <p>实例面线程安全：`WriteKind` 闭集（CREATE 新建 / OVERWRITE_UNCHANGED
 * 覆盖未变 / OVERWRITE_CHANGED 覆盖已变）+record+census+changedShare
 * （无样本 −1）+resetForTest。分类由宿主（写入路径）判定后喂入——纯读面。
 *
 * @since 1.0.0
 */
public final class FileWriteKindStats {

    /** 写型闭集。 */
    public enum WriteKind { CREATE, OVERWRITE_UNCHANGED, OVERWRITE_CHANGED }

    private final Map<WriteKind, AtomicLong> counters = new EnumMap<>(WriteKind.class);

    /** 默认构造。 */
    public FileWriteKindStats() {
        for (WriteKind kind : WriteKind.values()) {
            counters.put(kind, new AtomicLong());
        }
    }

    /** 记一次写入。 */
    public void record(WriteKind kind) {
        counters.get(kind).incrementAndGet();
    }

    /**
     * @param total             写入总数
     * @param creates           新建数
     * @param overwriteUnchanged 覆盖未变数
     * @param overwriteChanged  覆盖已变数
     * @param changedShare      实际变更占比 (creates+changed)/total；无样本 −1
     */
    public record WriteCensus(long total, long creates, long overwriteUnchanged,
                              long overwriteChanged, double changedShare) {
    }

    /** 快照。 */
    public WriteCensus census() {
        long creates = counters.get(WriteKind.CREATE).get();
        long same = counters.get(WriteKind.OVERWRITE_UNCHANGED).get();
        long changed = counters.get(WriteKind.OVERWRITE_CHANGED).get();
        long total = creates + same + changed;
        double share = total == 0 ? -1d : (double) (creates + changed) / total;
        return new WriteCensus(total, creates, same, changed, share);
    }

    /** 测试归零。 */
    public void resetForTest() {
        counters.values().forEach(c -> c.set(0));
    }
}

package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import java.util.ArrayList;
import java.util.List;

/**
 * Group Commit 组提交（spec 5013 / T6127 / impl 2164）——
 * WAL 一次落盘合并多事务思想（PostgreSQL/InnoDB group
 * commit）：`append` 进**当前组**并分配严格递增 LSN；`sync()`
 * 一次持久化整组（GroupSync 区间）——多事务合并一次 fsync，
 * 吞吐不被设备延迟钳死；`durableUpto()` 持久上沿单调不减
 * （哪些记录已落盘可证）。无持久上沿（落盘不可知）的病解。
 *
 * <p>与 GroupCommitAccounting（core/fs 记账面）同族不同面。
 */
public final class GroupCommitLog {

    /**
     * 一次落盘的组区间。
     *
     * @param records 落盘记录数（空组为 0）
     * @param fromLsn 组内最小 LSN（空组为 -1）
     * @param toLsn 组内最大 LSN（空组为 -1）
     */
    public record GroupSync(int records, long fromLsn, long toLsn) {
    }

    private final List<String> openGroup = new ArrayList<>();
    private long lastLsn;
    private long durableUpto;

    /** 追加记录进当前组（返回分配的 LSN；null/空 record fail-fast）。 */
    public long append(String record) {
        if (record == null || record.isEmpty()) {
            throw new IllegalArgumentException("record 非空");
        }
        openGroup.add(record);
        return ++lastLsn;
    }

    /** 一次落盘当前组（空组空同步——上沿不变）。 */
    public GroupSync sync() {
        if (openGroup.isEmpty()) {
            return new GroupSync(0, -1, -1);
        }
        long fromLsn = durableUpto + 1;
        long toLsn = lastLsn;
        int records = openGroup.size();
        durableUpto = toLsn;
        openGroup.clear();
        return new GroupSync(records, fromLsn, toLsn);
    }

    /** 已持久上沿读数（单调不减）。 */
    public long durableUpto() {
        return durableUpto;
    }

    /** 当前组内积压记录数读数。 */
    public int openGroupSize() {
        return openGroup.size();
    }

    /** 已分配最大 LSN 读数。 */
    public long lastLsn() {
        return lastLsn;
    }
}

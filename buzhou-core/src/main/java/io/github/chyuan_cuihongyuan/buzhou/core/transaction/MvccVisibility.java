package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import java.util.HashSet;
import java.util.Set;

/**
 * MVCC 快照可见性（spec 4040 / T6081 / impl 2141）——
 * PostgreSQL 快照口径（xmin/xmax + 快照 xmax + 在飞集）：行
 * 版本带创建者 xmin 与删除者 xmax，读侧以**不可变快照**判定
 * 可见性——读不阻塞写、写不阻塞读；repeatable read 免费成立
 * （同快照同判定，纯函数）。
 *
 * <p>可见性（XidInMVCCSnapshot 简化口径）：
 * <ul>
 *   <li>创建者可见 = xmin 有效 ∧ xmin &lt; snap.xmax ∧ 不在飞 ∧ 未中止；</li>
 *   <li>删除者不存在 = xmax 缺省(0) ∨ xmax ≥ snap.xmax ∨ 在飞 ∨ 已中止
 *    （删除晚于快照/未提交/回滚都对读者不存在）。</li>
 * </ul>
 *
 * <p>嵌套 {@link Snapshot}/{@link RowVersion} 不另立面。与
 * IdempotencyKeyGuard 互补：读侧一致性 vs 写侧去重。
 */
public final class MvccVisibility {

    /** 无效删除者（行未被删除标记）。 */
    public static final long NO_DELETER = 0L;

    private final Set<Long> inFlight = new HashSet<>();
    private final Set<Long> aborted = new HashSet<>();
    private long nextTxId = 1L;

    /** 分配新事务 id（在飞）。 */
    public long begin() {
        long txId = nextTxId++;
        inFlight.add(txId);
        return txId;
    }

    /** 提交（未知/重复/已中止 fail-fast）。 */
    public void commit(long txId) {
        requireInFlight(txId, "commit");
        inFlight.remove(txId);
    }

    /** 中止（未知/重复/已中止 fail-fast）。 */
    public void abort(long txId) {
        requireInFlight(txId, "abort");
        inFlight.remove(txId);
        aborted.add(txId);
    }

    /** 捕获不可变快照（xmax = 下一未分配号）。 */
    public Snapshot snapshot() {
        return new Snapshot(nextTxId, Set.copyOf(inFlight), Set.copyOf(aborted));
    }

    /**
     * 行版本在快照下的可见性（纯函数——同快照同判定）。
     *
     * @param snapshot 事务开始时捕获的快照
     * @param version 行版本（xmin=创建者、xmax=删除者或 0）
     */
    public static boolean visible(Snapshot snapshot, RowVersion version) {
        if (version.xmin() <= 0 || version.xmin() >= snapshot.xmax()
                || snapshot.inFlight().contains(version.xmin())
                || snapshot.aborted().contains(version.xmin())) {
            return false;   // 创建者对本快照不存在
        }
        long deleter = version.xmax();
        return deleter == NO_DELETER || deleter >= snapshot.xmax()
                || snapshot.inFlight().contains(deleter)
                || snapshot.aborted().contains(deleter);   // 删除者对本快照不存在
    }

    private void requireInFlight(long txId, String action) {
        if (!inFlight.contains(txId)) {
            throw new IllegalArgumentException("事务 " + action + " 需在飞 id：" + txId);
        }
    }

    /**
     * 不可变 MVCC 快照。
     *
     * @param xmax 捕获时的下一未分配事务号（判定边界）
     * @param inFlight 捕获时在飞事务集
     * @param aborted 捕获时已知中止事务集
     */
    public record Snapshot(long xmax, Set<Long> inFlight, Set<Long> aborted) {
    }

    /**
     * 行版本。
     *
     * @param xmin 创建者事务号（&gt;0）
     * @param xmax 删除者事务号（0=未删除）
     */
    public record RowVersion(long xmin, long xmax) {

        /** 未删除行版本。 */
        public static RowVersion created(long xmin) {
            return new RowVersion(xmin, NO_DELETER);
        }
    }
}

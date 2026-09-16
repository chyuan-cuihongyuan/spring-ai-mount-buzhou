package io.github.chyuan_cuihongyuan.buzhou.spill;

import java.util.ArrayList;
import java.util.List;

/**
 * 可冻结分段缓冲（spec 2040 / T3183 / impl 1591）——LSM memtable 不可
 * 变段切换思想：可变段写满即封冻为不可变段（冻结后只读——并发快照
 * 安全），新开空可变段续写；flush 以段为单位取走（drainFrozen——
 * 「不可变段整段持久化」的 LSM 节奏，而非逐条写放大）。全量快照 =
 * 冻结段序 + 可变段尾。
 *
 * <p>synchronized 小临界区；元素非空契约。
 */
public final class FreezableBuffer<T> {

    private final int segmentCapacity;
    private final List<List<T>> frozenSegments = new ArrayList<>();
    private List<T> mutableSegment = new ArrayList<>();

    /** 契约：segmentCapacity ≥ 1（fail-fast）。 */
    public FreezableBuffer(int segmentCapacity) {
        if (segmentCapacity < 1) {
            throw new IllegalArgumentException("segmentCapacity 须 ≥ 1：" + segmentCapacity);
        }
        this.segmentCapacity = segmentCapacity;
    }

    /** 追加：可变段满即自动封冻开新段。契约：item 非空。 */
    public synchronized void append(T item) {
        if (item == null) {
            throw new IllegalArgumentException("item 不能为 null");
        }
        if (mutableSegment.size() >= segmentCapacity) {
            freeze();
        }
        mutableSegment.add(item);
    }

    /** 手动封冻（可变段非空才产生新冻结段；空封冻 no-op）。 */
    public synchronized void freeze() {
        if (!mutableSegment.isEmpty()) {
            frozenSegments.add(mutableSegment);
            mutableSegment = new ArrayList<>(segmentCapacity);
        }
    }

    /** 取走全部冻结段（flush 语义——取走后清空，未持久化的可变段保留）。 */
    public synchronized List<List<T>> drainFrozen() {
        List<List<T>> drained = new ArrayList<>(frozenSegments);
        frozenSegments.clear();
        return drained;
    }

    /** 全量快照：冻结段序 + 可变段尾（追加序稳定）。 */
    public synchronized List<T> snapshot() {
        List<T> all = new ArrayList<>();
        frozenSegments.forEach(all::addAll);
        all.addAll(mutableSegment);
        return all;
    }

    /** 冻结段数（待持久化积压面）。 */
    public synchronized int frozenCount() {
        return frozenSegments.size();
    }

    /** 可变段当前占用。 */
    public synchronized int mutableSize() {
        return mutableSegment.size();
    }

    /** 总元素数（冻结 + 可变）。 */
    public synchronized int totalSize() {
        return frozenSegments.stream().mapToInt(List::size).sum() + mutableSegment.size();
    }
}

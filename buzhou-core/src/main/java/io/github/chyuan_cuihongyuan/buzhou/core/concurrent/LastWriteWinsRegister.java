package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

/**
 * 最后写入胜利寄存器（spec 2006 / T3113 / impl 1557）——Dynamo/CRDT
 * LWW 思想：多写者并发下寄存器值由 (timestamp, writerId) 字典序最大者
 * 胜——时间戳平局以 writerId 字典序确定性仲裁（Dynamo 论文惯例，无
 * 随机、无本地时钟依赖）；迟到旧写被拒并计数（乱序网络对账面）。
 *
 * <p>synchronized 小临界区；写者视角 put 返回是否采纳。
 */
public final class LastWriteWinsRegister<T> {

    /** 带版本口径的值（merge 跨实例传递的载体）。 */
    public record Timestamped<T>(T value, long timestamp, String writerId) {
    }

    private Timestamped<T> current;
    private long conflicts;
    private long superseded;

    /**
     * 写入：新写 (ts, writer) 字典序 &gt; 当前则采纳（true）；
     * &lt; 则拒（false——迟到旧写，{@link #supersededCount()} 计数）；
     * 完全相等幂等（true，无冲突计数）；ts 相等 writer 不同（或同
     * writer 不同 value）不可判定——按序仲裁保留现值并计冲突。
     * 契约：value/writerId 非空、timestamp ≥ 0（fail-fast）。
     */
    public synchronized boolean put(T value, long timestamp, String writerId) {
        if (value == null || writerId == null) {
            throw new IllegalArgumentException("value 与 writerId 不能为 null");
        }
        if (timestamp < 0) {
            throw new IllegalArgumentException("timestamp 须 ≥ 0：" + timestamp);
        }
        if (current == null) {
            current = new Timestamped<>(value, timestamp, writerId);
            return true;
        }
        int cmp = Long.compare(timestamp, current.timestamp());
        if (cmp > 0 || (cmp == 0 && writerId.compareTo(current.writerId()) > 0)) {
            current = new Timestamped<>(value, timestamp, writerId);
            if (cmp == 0) {
                conflicts++;
            }
            return true;
        }
        if (cmp == 0 && writerId.equals(current.writerId())) {
            if (value.equals(current.value())) {
                return true; // 完全幂等
            }
            conflicts++; // 同写者同时戳异值——不可判定，first-wins
            return false;
        }
        superseded++; // 迟到旧写（或平局落败者）
        return false;
    }

    /** 跨实例合并：对端 Timestamped 以同语义写入。 */
    public synchronized boolean merge(Timestamped<T> other) {
        if (other == null) {
            throw new IllegalArgumentException("other 不能为 null");
        }
        return put(other.value(), other.timestamp(), other.writerId());
    }

    /** 当前值与版本口径（空寄存器为 null——调用方判空）。 */
    public synchronized Timestamped<T> current() {
        return current;
    }

    /** 平局冲突计数（仲裁发生即+1——网络对称重放/时钟平局对账面）。 */
    public synchronized long conflictCount() {
        return conflicts;
    }

    /** 迟到旧写被拒计数（乱序到达对账面）。 */
    public synchronized long supersededCount() {
        return superseded;
    }
}

package io.github.chyuan_cuihongyuan.buzhou.core.transaction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Wait-Die / Wound-Wait 死锁预防时序裁决（spec 5030 / T6161 /
 * impl 2181）——PostgreSQL/DB2 两阶段锁死锁预防经典思想：
 * 事务以**开始时间戳定年龄**，资源冲突时按模式裁决——
 * WOUND_WAIT：年长请求者枪伤（中止）年轻持有者并接管，
 * 年轻请求者等待；WAIT_DIE：年长请求者等待，年轻请求者
 * 自裁（中止）——两者都保证等待图单向（只等更年轻/只等
 * 更年长），环不可形成→**死锁预防面**（无死锁检测与
 * 回滚风暴）。确定性无时间依赖（时间戳显式注入）。
 *
 * <p>与 TwoPhaseCoordinator（spec 5016）同族不同面：跨资源
 * 原子提交 vs 并发冲突时序裁决；与 FencingTokenGuard（S5）
 * 不同面：锁安全世代守卫 vs 死锁预防。
 */
public final class WoundWaitGate {

    /** 裁决模式（构造定构）。 */
    public enum Mode {
        /** 年长枪伤年轻持有者；年轻请求者等待。 */
        WOUND_WAIT,
        /** 年长请求者等待；年轻请求者自裁。 */
        WAIT_DIE
    }

    /** 一次冲突裁决结果。 */
    public enum Verdict {
        /** 资源空闲（或首次授予）——请求者成为持有者。 */
        GRANTED,
        /** 请求者等待（入等待队列）。 */
        WAIT,
        /** 请求者自裁（被中止并注销）。 */
        ABORT_REQUESTOR,
        /** 持有者被枪伤（被中止注销；请求者接管资源）。 */
        ABORT_HOLDER
    }

    private static final class Transaction {
        long startTimestamp;
        final List<String> heldResources = new ArrayList<>();
        final List<String> waitingFor = new ArrayList<>();
    }

    private final Mode mode;
    private final Map<String, Transaction> transactions = new HashMap<>();
    private final Map<String, TreeMap<Long, String>> holders = new HashMap<>();
    private final Map<String, TreeMap<Long, String>> waiters = new HashMap<>();
    private long abortedCount;

    /** 定构（裁决模式定构——同类门同模式）。 */
    public WoundWaitGate(Mode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("mode 非空");
        }
        this.mode = mode;
    }

    /** 事务注册（开始时间戳显式注入——同时间戳并列按 id 字典序 tie-break）。 */
    public void begin(String txnId, long startTimestamp) {
        requireId(txnId, "txnId");
        if (transactions.containsKey(txnId)) {
            throw new IllegalArgumentException("事务已注册：" + txnId);
        }
        Transaction txn = new Transaction();
        txn.startTimestamp = startTimestamp;
        transactions.put(txnId, txn);
    }

    /**
     * 资源申请：空闲即授予；冲突按模式+时间戳裁决（年长 =
     * 时间戳小；并列 id 字典序小者为年长）。
     */
    public Verdict acquire(String txnId, String resource) {
        requireId(txnId, "txnId");
        requireId(resource, "resource");
        Transaction requestor = requireTxn(txnId);
        TreeMap<Long, String> holderMap = holders.get(resource);
        if (holderMap == null || holderMap.isEmpty()) {
            grant(txnId, resource);
            return Verdict.GRANTED;
        }
        Map.Entry<Long, String> holderEntry = holderMap.firstEntry();
        if (holderEntry.getValue().equals(txnId)) {
            throw new IllegalArgumentException("事务已持有资源：" + txnId + "@" + resource);
        }
        boolean requestorOlder = isOlder(requestor.startTimestamp, txnId,
                holderEntry.getKey(), holderEntry.getValue());
        if (mode == Mode.WOUND_WAIT) {
            if (requestorOlder) {
                abort(holderEntry.getValue());
                grant(txnId, resource);
                return Verdict.ABORT_HOLDER;
            }
            waiters.computeIfAbsent(resource, k -> new TreeMap<>())
                    .put(waitKey(requestor), txnId);
            requestor.waitingFor.add(resource);
            return Verdict.WAIT;
        }
        if (requestorOlder) {
            waiters.computeIfAbsent(resource, k -> new TreeMap<>())
                    .put(waitKey(requestor), txnId);
            requestor.waitingFor.add(resource);
            return Verdict.WAIT;
        }
        abort(txnId);
        return Verdict.ABORT_REQUESTOR;
    }

    /**
     * 释放资源（仅持有者可释放）；等待者中年长者接管
     * （时间戳最小，并列先到序）。
     */
    public String release(String txnId, String resource) {
        requireId(txnId, "txnId");
        requireId(resource, "resource");
        TreeMap<Long, String> holderMap = holders.get(resource);
        if (holderMap == null || !holderMap.containsValue(txnId)) {
            throw new IllegalArgumentException("释放者非持有者：" + txnId + "@" + resource);
        }
        holderMap.values().removeIf(txnId::equals);
        String granted = handoff(resource);
        if (holderMap.isEmpty() && granted == null) {
            holders.remove(resource);
        }
        return granted;
    }

    /** 事务收尾（提交或中止同路径）：注销并释放全部资源。 */
    public void finish(String txnId) {
        requireId(txnId, "txnId");
        Transaction txn = requireTxn(txnId);
        for (String resource : List.copyOf(txn.heldResources)) {
            release(txnId, resource);
        }
        for (String resource : List.copyOf(txn.waitingFor)) {
            TreeMap<Long, String> queue = waiters.get(resource);
            if (queue != null) {
                queue.values().removeIf(txnId::equals);
            }
        }
        transactions.remove(txnId);
    }

    /** 资源当前持有者读数（时间戳序；无持有者空表）。 */
    public List<String> holdersOf(String resource) {
        TreeMap<Long, String> holderMap = holders.get(resource);
        return holderMap == null ? List.of() : List.copyOf(holderMap.values());
    }

    /** 资源等待者读数（年长优先序）。 */
    public List<String> waitersOf(String resource) {
        TreeMap<Long, String> queue = waiters.get(resource);
        return queue == null ? List.of() : List.copyOf(queue.values());
    }

    /** 已中止事务数读数（枪伤+自裁合计——预防的代价诚实可见）。 */
    public long abortedCount() {
        return abortedCount;
    }

    /** 在册事务数读数。 */
    public int transactionCount() {
        return transactions.size();
    }

    private void grant(String txnId, String resource) {
        holders.computeIfAbsent(resource, k -> new TreeMap<>())
                .put(grantKey(transactions.get(txnId)), txnId);
        transactions.get(txnId).heldResources.add(resource);
    }

    private String handoff(String resource) {
        TreeMap<Long, String> queue = waiters.get(resource);
        if (queue == null || queue.isEmpty()) {
            return null;
        }
        Map.Entry<Long, String> next = queue.firstEntry();
        queue.pollFirstEntry();
        String woken = next.getValue();
        transactions.get(woken).waitingFor.remove(resource);
        grant(woken, resource);
        if (queue.isEmpty()) {
            waiters.remove(resource);
        }
        return woken;
    }

    private void abort(String txnId) {
        finish(txnId);
        abortedCount++;
    }

    private long waitKey(Transaction txn) {
        return txn.startTimestamp;
    }

    private long grantKey(Transaction txn) {
        return txn.startTimestamp;
    }

    private boolean isOlder(long tsA, String idA, long tsB, String idB) {
        if (tsA != tsB) {
            return tsA < tsB;
        }
        return idA.compareTo(idB) < 0;
    }

    private Transaction requireTxn(String txnId) {
        Transaction txn = transactions.get(txnId);
        if (txn == null) {
            throw new IllegalArgumentException("事务未注册：" + txnId);
        }
        return txn;
    }

    private static void requireId(String id, String field) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException(field + " 非空");
        }
    }
}

package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Deficit Round Robin 亏空调度（spec 5027 / T6155 / impl 2178）
 * ——Shreedhar-Varghese DRR 思想：轮转各队 `deficit +=
 * quantum`，队头 size ≤ deficit 则发出并扣减；队空则亏空清零
 * 跳过；亏空跨轮结转——小流量积少成多不被大包压死。纯轮询
 * 按条数（字节不公平）的病解；确定性不阻塞（单轮无服务返回
 * null，调用方稍后再试）。
 *
 * <p>与 WeightedRoundRobin（S27）同族不同面：权重平滑 vs
 * 字节亏空。
 */
public final class DeficitRoundRobin {

    private final int quantum;
    private final int maxQueueBytes;
    private final List<String> queueOrder = new ArrayList<>();
    private final Map<String, Deque<Payload>> queues = new LinkedHashMap<>();
    private final Map<String, Integer> deficits = new LinkedHashMap<>();
    private int cursor;

    /**
     * 定构。
     *
     * @param queueIds 队列 id（注册序即轮转序）
     * @param quantum 每轮亏空增量（字节）
     * @param maxQueueBytes 单队列字节容量
     */
    public DeficitRoundRobin(List<String> queueIds, int quantum, int maxQueueBytes) {
        if (queueIds == null || queueIds.isEmpty() || quantum <= 0 || maxQueueBytes <= 0) {
            throw new IllegalArgumentException("队列非空且 quantum/maxQueueBytes>0："
                    + quantum + "/" + maxQueueBytes);
        }
        this.quantum = quantum;
        this.maxQueueBytes = maxQueueBytes;
        for (String queueId : queueIds) {
            if (queueId == null || queueId.isEmpty() || queues.containsKey(queueId)) {
                throw new IllegalArgumentException("队列 id 非空且不重复：" + queueId);
            }
            queues.put(queueId, new ArrayDeque<>());
            deficits.put(queueId, 0);
            queueOrder.add(queueId);
        }
    }

    /** 入队（未知队列/size≤0 fail-fast；队满字节容量返回 false）。 */
    public boolean enqueue(String queueId, String payloadId, int sizeBytes) {
        requireQueue(queueId);
        if (payloadId == null || payloadId.isEmpty()) {
            throw new IllegalArgumentException("payloadId 非空");
        }
        if (sizeBytes <= 0) {
            throw new IllegalArgumentException("sizeBytes>0：" + sizeBytes);
        }
        Deque<Payload> queue = queues.get(queueId);
        int used = queue.stream().mapToInt(Payload::sizeBytes).sum();
        if (used + sizeBytes > maxQueueBytes) {
            return false;   // 容量守恒
        }
        queue.addLast(new Payload(payloadId, sizeBytes));
        return true;
    }

    /** 轮转服务一个负载（单轮无可服务返回 null——确定性不阻塞）。 */
    public String dequeue() {
        for (int visited = 0; visited < queueOrder.size(); visited++) {
            String queueId = queueOrder.get(cursor);
            Deque<Payload> queue = queues.get(queueId);
            if (queue.isEmpty()) {
                deficits.put(queueId, 0);   // 队空——亏空清零
                cursor = advance(cursor);
                continue;
            }
            int deficit = deficits.get(queueId) + quantum;
            deficits.put(queueId, deficit);
            Payload head = queue.peekFirst();
            if (head.sizeBytes() <= deficit) {
                queue.pollFirst();
                deficits.put(queueId, deficit - head.sizeBytes());
                cursor = advance(cursor);
                return head.payloadId();
            }
            cursor = advance(cursor);   // 队头放不下——亏空结转看下队
        }
        return null;
    }

    /** 亏空读数（未知队列 fail-fast）。 */
    public int deficitOf(String queueId) {
        requireQueue(queueId);
        return deficits.get(queueId);
    }

    /** 队列字节数读数。 */
    public int queueBytes(String queueId) {
        requireQueue(queueId);
        return queues.get(queueId).stream().mapToInt(Payload::sizeBytes).sum();
    }

    /** 队列数读数。 */
    public int size() {
        return queues.size();
    }

    private int advance(int position) {
        return (position + 1) % queueOrder.size();
    }

    private void requireQueue(String queueId) {
        if (queueId == null || !queues.containsKey(queueId)) {
            throw new IllegalArgumentException("未知队列：" + queueId);
        }
    }

    /** 队列负载。 */
    private record Payload(String payloadId, int sizeBytes) {
    }
}

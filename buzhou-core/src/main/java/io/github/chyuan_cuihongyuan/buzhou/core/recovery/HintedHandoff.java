package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Hinted Handoff 暂代投递（spec 5010 / T6121 / impl 2161）——
 * Cassandra hinted handoff 思想：目标节点下线期间投递转记为
 * hint（按目标 FIFO 记账，主路径不阻塞不丢失），恢复后
 * `markUp` 按 FIFO 回放并清队。直接丢弃（恢复后数据缺口）与
 * 无休止阻塞重试（拖死主路径）的病解。
 *
 * <p>markDown/markUp 为显式状态机（重复转换 IAE fail-fast）；
 * 恢复后再下线则新 hint 独立累积。与持久化 Outbox（effort #6）
 * 同族不同面：事件外发持久队列 vs 目标级暂代回放。
 */
public final class HintedHandoff {

    private final Set<String> knownTargets = new HashSet<>();
    private final Set<String> downTargets = new HashSet<>();
    private final Map<String, Deque<String>> hints = new HashMap<>();

    /** 注册投递目标（重复注册 IAE fail-fast）。 */
    public void registerTarget(String target) {
        checkTarget(target);
        if (!knownTargets.add(target)) {
            throw new IllegalArgumentException("重复注册目标：" + target);
        }
    }

    /**
     * 投递路由：目标健康走正常投递（false——无 hint）；下线转
     * 记 hint（true）。未注册目标 IAE fail-fast。
     */
    public boolean route(String target, String payloadId) {
        checkTarget(target);
        if (payloadId == null || payloadId.isEmpty()) {
            throw new IllegalArgumentException("payloadId 非空");
        }
        if (!knownTargets.contains(target)) {
            throw new IllegalArgumentException("未注册目标：" + target);
        }
        if (!downTargets.contains(target)) {
            return false;   // 正常投递——无 hint
        }
        hints.computeIfAbsent(target, key -> new ArrayDeque<>()).addLast(payloadId);
        return true;
    }

    /** 标记下线（未注册/已下线 IAE fail-fast）。 */
    public void markDown(String target) {
        checkTarget(target);
        if (!knownTargets.contains(target)) {
            throw new IllegalArgumentException("未注册目标：" + target);
        }
        if (!downTargets.add(target)) {
            throw new IllegalArgumentException("已下线不可重复标记：" + target);
        }
    }

    /**
     * 标记恢复并回放暂代清单（FIFO；未注册/未下线 IAE fail-fast）。
     *
     * @return 按排队序的暂代 payloadId（回放后清队）
     */
    public List<String> markUp(String target) {
        checkTarget(target);
        if (!knownTargets.contains(target)) {
            throw new IllegalArgumentException("未注册目标：" + target);
        }
        if (!downTargets.remove(target)) {
            throw new IllegalArgumentException("未下线不可恢复：" + target);
        }
        Deque<String> queue = hints.remove(target);
        return queue == null ? List.of() : List.copyOf(queue);
    }

    /** 某目标暂代积压数读数。 */
    public int hintCountOf(String target) {
        checkTarget(target);
        Deque<String> queue = hints.get(target);
        return queue == null ? 0 : queue.size();
    }

    /** 目标是否下线。 */
    public boolean isDown(String target) {
        checkTarget(target);
        return downTargets.contains(target);
    }

    private static void checkTarget(String target) {
        if (target == null || target.isEmpty()) {
            throw new IllegalArgumentException("target 非空");
        }
    }
}

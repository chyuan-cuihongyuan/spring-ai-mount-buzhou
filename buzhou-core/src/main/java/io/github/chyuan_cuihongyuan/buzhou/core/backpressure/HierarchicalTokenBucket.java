package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 层级令牌桶（spec 2063 / T3227 / impl 1614）——Linux HTB（Hierarchical
 * Token Bucket）思想：两级限流——父桶定总额度，子桶在父剩余内各自
 * 限流（子桶有再充值但父桶空 → 借不到——**总额硬顶**）；子桶间不
 * 匀借（隔离——一个租户打满自己的子桶不影响他人，但合打不破父顶）。
 * 租户（子）× 产品线（父）两级限流的原语底座。
 *
 * <p>同步小临界区；时间外注入（确定性可回放）。
 */
public final class HierarchicalTokenBucket {

    private final double parentCapacity;
    private double parentTokens;
    private final Map<String, Double> childCapacities = new LinkedHashMap<>();
    private final Map<String, Double> childTokens = new HashMap<>();

    /** 契约：parentCapacity ≥ 1（fail-fast）。 */
    public HierarchicalTokenBucket(double parentCapacity) {
        if (!(parentCapacity >= 1) || Double.isNaN(parentCapacity)) {
            throw new IllegalArgumentException("parentCapacity 须 ≥ 1：" + parentCapacity);
        }
        this.parentCapacity = parentCapacity;
        this.parentTokens = parentCapacity;
    }

    /** 注册子桶（容量 ≤ 父容量建议不强制——合容量可超父顶靠父顶兜底）。契约：id 非空非重复、capacity ≥ 0。 */
    public synchronized void registerChild(String childId, double capacity) {
        if (childId == null || childId.isBlank()) {
            throw new IllegalArgumentException("childId 不能为空");
        }
        if (childCapacities.containsKey(childId)) {
            throw new IllegalArgumentException("子桶已注册：" + childId);
        }
        if (!(capacity >= 0) || Double.isNaN(capacity)) {
            throw new IllegalArgumentException("capacity 须 ≥ 0：" + capacity);
        }
        childCapacities.put(childId, capacity);
        childTokens.put(childId, capacity);
    }

    /**
     * 子桶取令牌：父剩余与子剩余**双闸**——任一不足即拒（父空=总额
     * 硬顶，子空=自限）；通过则双扣。契约：childId 已注册、amount ≥ 0。
     */
    public synchronized boolean tryConsume(String childId, double amount) {
        requireRegistered(childId);
        if (amount < 0 || Double.isNaN(amount)) {
            throw new IllegalArgumentException("amount 须 ≥ 0：" + amount);
        }
        if (parentTokens < amount || childTokens.get(childId) < amount) {
            return false;
        }
        parentTokens -= amount;
        childTokens.merge(childId, -amount, Double::sum);
        return true;
    }

    /** 再充值（周期补币——时间驱动归调用方）：父补至容量，各子补至容量。契约：tokens ≥ 0。 */
    public synchronized void refill(double parentAmount, Map<String, Double> childAmounts) {
        if (!(parentAmount >= 0) || Double.isNaN(parentAmount)) {
            throw new IllegalArgumentException("parentAmount 须 ≥ 0：" + parentAmount);
        }
        if (childAmounts == null) {
            throw new IllegalArgumentException("childAmounts 不能为 null");
        }
        parentTokens = Math.min(parentCapacity, parentTokens + parentAmount);
        childAmounts.forEach((id, amt) -> {
            requireRegistered(id);
            if (amt == null || !(amt >= 0) || Double.isNaN(amt)) {
                throw new IllegalArgumentException("子补币须 ≥ 0 非 null");
            }
            childTokens.merge(id, Math.min(childCapacities.get(id) - childTokens.get(id), amt),
                    Double::sum);
        });
    }

    /** 读数：父剩余 / 各子剩余（对账面）。 */
    public synchronized Map<String, Double> snapshot() {
        Map<String, Double> snap = new LinkedHashMap<>();
        snap.put("(parent)", parentTokens);
        childCapacities.forEach((id, cap) -> snap.put(id, childTokens.get(id)));
        return snap;
    }

    private void requireRegistered(String childId) {
        if (childId == null || !childCapacities.containsKey(childId)) {
            throw new IllegalArgumentException("子桶未注册：" + childId);
        }
    }
}

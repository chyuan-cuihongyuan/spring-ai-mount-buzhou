package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 祖先费率打包（spec 4032 / T6065 / impl 2133）——CPFP 思想
 * （Bitcoin Child Pays For Parent 祖先包费率）：依赖图任务的
 * 排名按**祖先闭包（含自身）∑fee/∑size** 聚合费率——高价值
 * 后继把低价值前置拉进批次（贪婪单笔排序让低费父永远滞留、
 * 高费子的依赖未满足谁也进不了批次的病解）。
 *
 * <p>共享祖先去重（多子不重复抬）；{@code packageOf} 祖先闭包
 * 拓扑序（父先子后）；{@code inclusionOrder} 贪心优先拓扑——
 * 每轮只从「父已全出」候选中取祖先费率最高者（并列 id 字典序，
 * 确定性可回放）；依赖图必须 DAG（环 fail-fast——Bitcoin
 * mempool 同约束）。
 *
 * <p>与 TopologicalSorter 同族不同问：偏序排程 vs 聚合价值
 * 驱动的打包序。
 */
public final class AncestorFeerate {

    /**
     * 依赖图节点（fee≥0、size>0；parents 为依赖前置）。
     *
     * @param id 节点唯一标识
     * @param fee 单笔价值（费）
     * @param size 单笔成本（尺寸）
     * @param parents 依赖前置 id 集
     */
    public record Tx(String id, long fee, long size, List<String> parents) {
    }

    private static final int VISITING = 1;
    private static final int DONE = 2;

    private final Map<String, Tx> txs = new LinkedHashMap<>();

    /** 注册节点（未知父引用/重复 id/非正 size/空 id fail-fast）。 */
    public void add(Tx tx) {
        if (tx == null || tx.id() == null || tx.id().isEmpty()) {
            throw new IllegalArgumentException("tx 与 id 非空");
        }
        if (tx.size() <= 0) {
            throw new IllegalArgumentException("size 需正：" + tx.id() + "/" + tx.size());
        }
        if (tx.fee() < 0) {
            throw new IllegalArgumentException("fee 需非负：" + tx.id() + "/" + tx.fee());
        }
        if (txs.containsKey(tx.id())) {
            throw new IllegalArgumentException("重复 id：" + tx.id());
        }
        for (String parent : tx.parents()) {
            if (!txs.containsKey(parent)) {
                throw new IllegalArgumentException("未知父引用：" + tx.id() + " → " + parent);
            }
        }
        txs.put(tx.id(), tx);
    }

    /**
     * 祖先包费率（含自身闭包 ∑fee/∑size；共享祖先去重）。
     *
     * @param id 节点 id（未注册 fail-fast）
     */
    public double ancestorFeerateOf(String id) {
        Closure closure = closureOf(id);
        return (double) closure.fee() / closure.size();
    }

    /** 祖先闭包拓扑序（父先子后；DFS 后序按父名字典序——确定性）。 */
    public List<String> packageOf(String id) {
        Closure closure = closureOf(id);
        return new ArrayList<>(closure.order());
    }

    /**
     * 打包序（CPFP 同式）：每轮取全池祖先费率最高者（并列 id
     * 字典序），其祖先闭包**整包入场**（父先子后）——子的高费
     * 把父拖进批次；前置先注册的 DAG-by-construction 约束下
     * 闭环构造不可达（未知父 fail-fast 即防线）。
     */
    public List<String> inclusionOrder() {
        List<String> order = new ArrayList<>(txs.size());
        Set<String> emitted = new LinkedHashSet<>();
        List<String> remaining = new ArrayList<>(txs.keySet());
        while (!remaining.isEmpty()) {
            String best = null;
            double bestFeerate = -1;
            for (String candidateId : remaining) {
                double feerate = ancestorFeerateOf(candidateId);
                if (best == null || feerate > bestFeerate
                        || (feerate == bestFeerate && candidateId.compareTo(best) < 0)) {
                    best = candidateId;
                    bestFeerate = feerate;
                }
            }
            for (String ancestor : packageOf(best)) {
                if (emitted.add(ancestor)) {
                    order.add(ancestor);
                    remaining.remove(ancestor);
                }
            }
        }
        return order;
    }

    /** 已注册节点数读数。 */
    public int size() {
        return txs.size();
    }

    /** 闭包求值：∑fee/∑size + 拓扑序 + 环检测（三色 DFS）。 */
    private Closure closureOf(String id) {
        Tx root = txs.get(id);
        if (root == null) {
            throw new IllegalArgumentException("未注册节点：" + id);
        }
        Map<String, Integer> state = new HashMap<>();
        List<String> order = new ArrayList<>();
        long[] totals = {0L, 0L};
        visit(id, state, order, totals);
        return new Closure(totals[0], totals[1], order);
    }

    private void visit(String id, Map<String, Integer> state, List<String> order, long[] totals) {
        Integer current = state.get(id);
        if (current != null) {
            if (current == VISITING) {
                throw new IllegalArgumentException("依赖环经：" + id);
            }
            return;   // DONE——共享祖先只计一次
        }
        state.put(id, VISITING);
        Tx tx = txs.get(id);
        List<String> parents = new ArrayList<>(tx.parents());
        parents.sort(Comparator.naturalOrder());
        for (String parent : parents) {
            visit(parent, state, order, totals);
        }
        state.put(id, DONE);
        order.add(id);
        totals[0] += tx.fee();
        totals[1] += tx.size();
    }

    private record Closure(long fee, long size, List<String> order) {
    }
}

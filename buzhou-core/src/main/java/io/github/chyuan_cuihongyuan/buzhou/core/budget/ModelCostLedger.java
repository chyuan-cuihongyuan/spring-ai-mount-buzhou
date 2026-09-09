package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 模型成本台账（spec 174 §A / T528，WandB/Langfuse cost tracking 借鉴）：
 * per-model micro-USD 累计的进程内有界表——「哪个模型在烧钱」的账单事实源
 * （与 TokenBudgetHook 的 per-session 成本口径正交：那是会话闸，这是全局账）。
 *
 * <p><b>口径</b>：整数 micro-USD（spec 16 同口径——无浮点漂移）；模型名封顶
 * {@value #MAX_MODELS} 折 {@code __overflow__}（新名不再扩张）；{@code reset()}
 * 窗口清零（export → reset 循环——与全导出族同纪律）；排序稳定
 * （microUsd 降序 + 名字典序）。
 */
public final class ModelCostLedger {

    /** 模型名封顶（防御面）。 */
    public static final int MAX_MODELS = 64;
    /** 封顶后新模型折入的计数键。 */
    public static final String OVERFLOW = "__overflow__";

    /** 单模型成本快照（账单行）。 */
    public record ModelCost(String model, long microUsd) {
    }

    /**
     * 价目快照（spec 314 / T619，复式记账——账单自含计价事实）：该模型最近一次
     * 记账时的单价（每百万 token USD，BigDecimal 原口径）。null 字段 = 无价目
     * （零成本行不被伪价污染）。
     */
    public record PricingSnapshot(java.math.BigDecimal inputPerMillion,
                                  java.math.BigDecimal outputPerMillion) {
    }

    private static final AtomicReference<ModelCostLedger> GLOBAL =
            new AtomicReference<>(new ModelCostLedger());

    private final Map<String, AtomicLong> microUsd = new ConcurrentHashMap<>();
    private final Map<String, PricingSnapshot> pricingByModel = new ConcurrentHashMap<>();
    /** spec 403 / T698：记账监听缝（预测器等订阅——记账即喂数，不在 hook 二次算成本）。 */
    private final java.util.List<java.util.function.Consumer<ModelCost>> listeners =
            new java.util.concurrent.CopyOnWriteArrayList<>();

    private ModelCostLedger() {
    }

    /** 独立实例（测试/宿主自管作用域）。 */
    public static ModelCostLedger create() {
        return new ModelCostLedger();
    }

    /** 全局默认实例（TokenBudgetHook 打点用——全局旋钮模式）。 */
    public static ModelCostLedger global() {
        return GLOBAL.get();
    }

    /** 测试替换/清理（null = 换新）。 */
    public static void install(ModelCostLedger ledger) {
        GLOBAL.set(ledger == null ? new ModelCostLedger() : ledger);
    }

    /** 记一次模型调用成本（microUsd ≥ 0；0 也记——「跑过零成本」是事实；无价目快照）。 */
    public void record(String model, long costMicroUsd) {
        record(model, costMicroUsd, null);
    }

    /**
     * 记一次模型调用成本 + 价目快照随单（spec 314）：快照 = 本次记账时该模型单价
     * （覆盖式——行示「最近一次记账时」的单价；行级逐笔记价归事件流，聚合面诚实边界）。
     */
    public void record(String model, long costMicroUsd, PricingSnapshot pricingSnapshot) {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("model must not be blank");
        }
        if (costMicroUsd < 0) {
            throw new IllegalArgumentException("costMicroUsd must be >= 0: " + costMicroUsd);
        }
        AtomicLong counter = microUsd.get(model);
        if (counter != null) {
            counter.addAndGet(costMicroUsd);
        } else if (microUsd.size() >= MAX_MODELS) {
            microUsd.computeIfAbsent(OVERFLOW, k -> new AtomicLong()).addAndGet(costMicroUsd);
            if (pricingSnapshot != null) {
                pricingByModel.put(OVERFLOW, pricingSnapshot);
            }
            fireListeners(OVERFLOW, costMicroUsd); // spec 403：OVERFLOW 路径同样喂数
            return;
        } else {
            microUsd.computeIfAbsent(model, k -> new AtomicLong()).addAndGet(costMicroUsd);
        }
        if (pricingSnapshot != null) {
            pricingByModel.put(model, pricingSnapshot);
        }
        fireListeners(model, costMicroUsd);
    }

    /** spec 403 / T698：订阅记账事件（每笔 record 触发一次；返回 this 便于链式注销引用）。 */
    public java.util.function.Consumer<ModelCost> addListener(java.util.function.Consumer<ModelCost> listener) {
        listeners.add(listener);
        return listener;
    }

    /** 注销（幂等）。 */
    public void removeListener(java.util.function.Consumer<ModelCost> listener) {
        listeners.remove(listener);
    }

    private void fireListeners(String model, long costMicroUsd) {
        if (listeners.isEmpty()) {
            return;
        }
        for (java.util.function.Consumer<ModelCost> listener : listeners) {
            try {
                listener.accept(new ModelCost(model, costMicroUsd));
            } catch (RuntimeException ignored) {
                // 监听器故障不阻断记账（观察面 fail-soft）
            }
        }
    }

    /** 该模型最近一次记账时的价目快照（未记价 = null——诚实空值）。 */
    public PricingSnapshot pricingOf(String model) {
        return pricingByModel.get(model);
    }

    /** 单模型累计（未见过 0——诚实空值）。 */
    public long costOf(String model) {
        AtomicLong counter = microUsd.get(model);
        return counter == null ? 0L : counter.get();
    }

    /** 成本排行 top-N（microUsd 降序，同名序字典——账单稳定序）。 */
    public List<ModelCost> topByCost(int n) {
        return microUsd.entrySet().stream()
                .sorted((a, b) -> {
                    int byCost = Long.compare(b.getValue().get(), a.getValue().get());
                    return byCost != 0 ? byCost : a.getKey().compareTo(b.getKey());
                })
                .limit(Math.max(0, n))
                .map(e -> new ModelCost(e.getKey(), e.getValue().get()))
                .toList();
    }

    /** 在册模型数（含 overflow 占位）。 */
    public int distinct() {
        return microUsd.size();
    }

    /** 总成本（含 overflow——账单总数列）。 */
    public long totalMicroUsd() {
        return microUsd.values().stream().mapToLong(AtomicLong::get).sum();
    }

    /** 窗口清零（export → reset 循环——每窗口一份账单；价目快照同清）。 */
    public void reset() {
        microUsd.clear();
        pricingByModel.clear();
    }
}

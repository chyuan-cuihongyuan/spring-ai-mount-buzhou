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

    private static final AtomicReference<ModelCostLedger> GLOBAL =
            new AtomicReference<>(new ModelCostLedger());

    private final Map<String, AtomicLong> microUsd = new ConcurrentHashMap<>();

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

    /** 记一次模型调用成本（microUsd ≥ 0；0 也记——「跑过零成本」是事实）。 */
    public void record(String model, long costMicroUsd) {
        if (model == null || model.isBlank()) {
            throw new IllegalArgumentException("model must not be blank");
        }
        if (costMicroUsd < 0) {
            throw new IllegalArgumentException("costMicroUsd must be >= 0: " + costMicroUsd);
        }
        AtomicLong counter = microUsd.get(model);
        if (counter != null) {
            counter.addAndGet(costMicroUsd);
            return;
        }
        if (microUsd.size() >= MAX_MODELS) {
            microUsd.computeIfAbsent(OVERFLOW, k -> new AtomicLong()).addAndGet(costMicroUsd);
            return;
        }
        microUsd.computeIfAbsent(model, k -> new AtomicLong()).addAndGet(costMicroUsd);
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

    /** 窗口清零（export → reset 循环——每窗口一份账单）。 */
    public void reset() {
        microUsd.clear();
    }
}

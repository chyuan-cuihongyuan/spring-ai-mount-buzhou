package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 成本归因台账（spec 334 / T659，Kubecost/OpenCost 按标签归因借鉴）：
 * 同一笔 microUsd 同时入 MODEL 维与 VIRTUAL_KEY 维——「换模型省钱」与
 * 「谁在花钱（chargeback）」两个问题一张台账都能答。
 *
 * <p><b>口径</b>：整数 micro-USD（spec 16 同口径——无浮点漂移）；share 为
 * 万分比整数（basis points，long 整除）；维值封顶 {@value #MAX_VALUES} 折
 * {@code __overflow__}（ModelCostLedger 同纪律）；无虚拟 key 归
 * {@code __unattributed__}（未接入部署的成本不蒸发——总账对得上全局模型账）；
 * {@code reset()} 窗口清零（export → reset 循环——导出族同纪律）。
 * 零成本也记（「跑过零成本」是归因事实）。
 */
public final class CostAttributionLedger {

    /** 维值封顶（防御面）。 */
    public static final int MAX_VALUES = 64;
    /** 封顶后新维值折入的计数键 / 无 key 诚实桶。 */
    public static final String OVERFLOW = "__overflow__";
    public static final String UNATTRIBUTED = "__unattributed__";

    /** 归因维度（开放扩展——skill/tool 等喂点就绪后追加）。 */
    public enum Dimension {
        MODEL, VIRTUAL_KEY
    }

    /** 一行归因报表（shareBp = 万分比整数，usd 由微元换算的人读字符串）。 */
    public record Attribution(Dimension dimension, String value, long microUsd,
                              long shareBp, String usd) {

        public Attribution {
            dimension = java.util.Objects.requireNonNull(dimension);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("value 非空");
            }
            usd = java.math.BigDecimal.valueOf(microUsd, 6).toPlainString();
        }
    }

    private static final AtomicReference<CostAttributionLedger> GLOBAL =
            new AtomicReference<>(new CostAttributionLedger());

    private final Map<Dimension, Map<String, AtomicLong>> microUsdByDimension =
            new ConcurrentHashMap<>();
    private final Map<Dimension, AtomicLong> totals = new ConcurrentHashMap<>();

    private CostAttributionLedger() {
    }

    /** 独立实例（测试/宿主自管作用域）。 */
    public static CostAttributionLedger create() {
        return new CostAttributionLedger();
    }

    /** 全局默认实例（TokenBudgetHook 打点用——全局旋钮模式）。 */
    public static CostAttributionLedger global() {
        return GLOBAL.get();
    }

    /** 测试替换/清理（null = 换新）。 */
    public static void install(CostAttributionLedger ledger) {
        GLOBAL.set(ledger == null ? new CostAttributionLedger() : ledger);
    }

    /** 记一笔（双维同入：MODEL 维 + VIRTUAL_KEY 维；virtualKey null → 诚实桶）。 */
    public void record(String model, String virtualKey, long costMicroUsd) {
        if (model == null || model.isBlank()) {
            model = UNATTRIBUTED;
        }
        accrue(Dimension.MODEL, model, costMicroUsd);
        accrue(Dimension.VIRTUAL_KEY,
                virtualKey == null || virtualKey.isBlank() ? UNATTRIBUTED : virtualKey,
                costMicroUsd);
    }

    /** 维报表（microUsd 降序 + 名字典序稳定；share 相对当维总额）。 */
    public List<Attribution> rollup(Dimension dimension) {
        long total = totalMicroUsd(dimension);
        Map<String, AtomicLong> values = microUsdByDimension.get(dimension);
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.entrySet().stream()
                .map(e -> new Attribution(dimension, e.getKey(),
                        e.getValue().get(),
                        total == 0 ? 0L : Math.floorDiv(e.getValue().get() * 10_000L, total),
                        null))
                .sorted(Comparator.comparingLong(Attribution::microUsd).reversed()
                        .thenComparing(Attribution::value))
                .toList();
    }

    /** 当维总额（micro-USD）。 */
    public long totalMicroUsd(Dimension dimension) {
        AtomicLong total = totals.get(dimension);
        return total == null ? 0L : total.get();
    }

    /** 窗口清零（export → reset 循环）。 */
    public void reset() {
        microUsdByDimension.clear();
        totals.clear();
    }

    private void accrue(Dimension dimension, String value, long costMicroUsd) {
        Map<String, AtomicLong> values = microUsdByDimension.computeIfAbsent(
                dimension, k -> new ConcurrentHashMap<>());
        // 封顶含溢出桶一槽：至多 MAX_VALUES-1 个实值 + __overflow__ = MAX_VALUES 个键
        String key = values.size() >= MAX_VALUES - 1 && !values.containsKey(value)
                ? OVERFLOW : value;
        values.computeIfAbsent(key, k -> new AtomicLong()).addAndGet(costMicroUsd);
        totals.computeIfAbsent(dimension, k -> new AtomicLong()).addAndGet(costMicroUsd);
    }
}

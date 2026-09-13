package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 定价表覆盖审计（spec 827 / T1155，LiteLLM model_prices 覆盖思想——被调用
 * 模型必须在价目库内否则成本归因静默漏计）：对「实际被调用的模型集合」与
 * 「定价表键集」做覆盖对账——未知模型列表+覆盖率——「哪些调用的成本没进
 * 账」结构化（归因缺口即预算失真源）。
 *
 * <p>匹配语义（宽进）：精确 → 忽略大小写 → 剥离 {@code provider/} 前缀后
 * 再比（LiteLLM 同款 {@code openai/gpt-4} 形态）；命中任一即算覆盖。
 * 未知列表典序封顶 {@value #MAX_LIST}（超出以 {@code …} 汇总行截断）。
 * 纯函数：键集由调用方自 PricingTable 采集（零侵入）。
 */
public final class PricingCoverageAudit {

    /** 未知列表封顶。 */
    public static final int MAX_LIST = 32;

    /** 不可变报告。 */
    public record Report(int calledModels, int coveredModels, double coverageRatio,
                         List<String> unknown) {
    }

    private PricingCoverageAudit() {
    }

    /** 覆盖审计：pricedKeys=定价表键集；called=实际被调用模型名（脏名忽略）。 */
    public static Report audit(Set<String> pricedKeys, Collection<String> called) {
        Set<String> priced = normalizeAll(pricedKeys);
        Set<String> unknown = new TreeSet<>();
        int calledCount = 0;
        int covered = 0;
        Set<String> seen = new HashSet<>();
        for (String model : called) {
            if (model == null || model.isBlank() || !seen.add(model)) {
                continue;
            }
            calledCount++;
            if (matches(priced, model)) {
                covered++;
            } else {
                unknown.add(model);
            }
        }
        double ratio = calledCount == 0 ? 1.0 : (double) covered / calledCount;
        List<String> list = new ArrayList<>(unknown);
        List<String> limited = list.size() > MAX_LIST
                ? withOverflowMarker(list) : list;
        return new Report(calledCount, covered, ratio, List.copyOf(limited));
    }

    private static List<String> withOverflowMarker(List<String> list) {
        List<String> cut = new ArrayList<>(list.subList(0, MAX_LIST));
        cut.add("…（共 " + list.size() + " 个未覆盖）");
        return cut;
    }

    private static boolean matches(Set<String> priced, String model) {
        if (priced.contains(model)) {
            return true;
        }
        String lower = model.toLowerCase(java.util.Locale.ROOT);
        if (priced.contains(lower)) {
            return true;
        }
        int slash = lower.indexOf('/');
        if (slash >= 0 && slash < lower.length() - 1) {
            return priced.contains(lower.substring(slash + 1));
        }
        return false;
    }

    private static Set<String> normalizeAll(Collection<String> keys) {
        Set<String> out = new HashSet<>();
        if (keys == null) {
            return out;
        }
        for (String key : keys) {
            if (key != null && !key.isBlank()) {
                out.add(key);
                out.add(key.toLowerCase(java.util.Locale.ROOT));
            }
        }
        return out;
    }
}

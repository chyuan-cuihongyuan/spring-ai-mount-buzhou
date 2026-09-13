package io.github.chyuan_cuihongyuan.buzhou.core.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * 配置默认偏离读数（spec 848 / T1197，Spring Boot configuration metadata
 * 思想扩散——「多少项偏离默认」是环境漂移的最小画像）：对 (键 → 当前值)
 * 与 (键 → 默认值) 做偏离对账——偏离项清单（含当前值）+偏离率——「这套
 * 环境改了哪些默认」结构化（ConfigDiff 是两次快照间 diff；本类是 vs 出厂默认）。
 *
 * <p>纯函数：默认值缺失的键不算偏离（无基线不裁决——诚实口径）；偏离清单
 * 典序封顶 {@value #MAX_LIST}；值比较走 String.equals（调用方先归一化类型）。
 */
public final class ConfigDeviationAudit {

    /** 偏离清单封顶。 */
    public static final int MAX_LIST = 32;

    /** 单项偏离行。 */
    public record Deviation(String key, String currentValue, String defaultValue) {
    }

    /** 不可变报告。 */
    public record Report(int configuredKeys, int deviatingKeys, double deviationRatio,
                         List<Deviation> deviations) {
    }

    private ConfigDeviationAudit() {
    }

    /** 偏离对账：currents 任一非空即计；defaults 缺键=无基线不裁决。 */
    public static Report audit(Map<String, String> currents, Map<String, String> defaults) {
        Objects.requireNonNull(currents, "currents");
        Objects.requireNonNull(defaults, "defaults");
        List<Deviation> deviations = new ArrayList<>();
        int configured = 0;
        for (Map.Entry<String, String> e : new TreeMap<>(currents).entrySet()) {
            String key = e.getKey();
            String current = e.getValue();
            if (key == null || key.isBlank() || current == null || current.isBlank()) {
                continue;
            }
            String defaultValue = defaults.get(key);
            if (defaultValue == null) {
                continue; // 无基线不裁决
            }
            configured++;
            if (!current.equals(defaultValue)) {
                deviations.add(new Deviation(key, current, defaultValue));
            }
        }
        List<Deviation> limited = deviations.size() > MAX_LIST
                ? List.copyOf(deviations.subList(0, MAX_LIST)) : List.copyOf(deviations);
        double ratio = configured == 0 ? 0 : (double) deviations.size() / configured;
        return new Report(configured, deviations.size(), ratio, limited);
    }
}

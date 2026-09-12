package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;

import java.util.Map;

/**
 * 工具入参限幅（spec 506 / T763，nginx client_max_body_size 思想——spec 31
 * 结果限幅的入站对称面）：模型拼出的超大入参（万行 JSON/整文件内联）执行前
 * 拦下，回喂结构化反馈（不回显超限入参——回显即重新入上下文），模型精简
 * 后重试而非炸下游。
 *
 * <p>与结果限幅（31）不同：<b>默认关闭</b>（-1，零默认行为变化——显式
 * opt-in）；拒绝而非截断（截断 JSON 入参必坏语义）。
 */
public final class ToolInputLimiter {

    private final int defaultLimit;
    private final Map<String, Integer> overrides;

    public ToolInputLimiter(int defaultLimit, Map<String, Integer> overrides) {
        if (defaultLimit < -1) {
            throw new IllegalArgumentException("input-limit 默认值须 >= -1（当前 " + defaultLimit + "）");
        }
        if (overrides != null && overrides.values().stream().anyMatch(v -> v != null && v < -1)) {
            throw new IllegalArgumentException("input-limit overrides 值须 >= -1");
        }
        this.defaultLimit = defaultLimit;
        this.overrides = overrides == null ? Map.of() : Map.copyOf(overrides);
    }

    /** 默认关闭档（零行为变化）。 */
    public static ToolInputLimiter disabled() {
        return new ToolInputLimiter(-1, Map.of());
    }

    /**
     * 入参超限检查：返回结构化反馈文案（工具名 + 超限量 + 精简指引），
     * 未超限返回 null。反馈不回显入参本身（回显即重新入上下文）。
     */
    public String violation(String toolName, String args) {
        int limit = limitFor(toolName);
        if (limit < 0 || args == null || args.length() <= limit) {
            return null;
        }
        BuzhouMetricsHolder.metrics().counter("buzhou.tools.input-rejected",
                "tool", toolName);
        return "[入参超限] 工具 " + toolName + " 入参 " + args.length()
                + " 字符超上限 " + limit + "——本次未执行。请精简参数"
                + "（缩小查询范围/分页提交/外链引用）后重试";
    }

    /** per-tool 生效上限（glob 覆盖优先——31 同法）；-1 = 不限。 */
    public int limitFor(String toolName) {
        if (defaultLimit < 0 && overrides.isEmpty()) {
            return -1;
        }
        for (Map.Entry<String, Integer> e : overrides.entrySet()) {
            if (globMatch(e.getKey(), toolName)) {
                return e.getValue();
            }
        }
        return defaultLimit;
    }

    /** 极简 glob：{@code *} 通配任意串（31 同法）。 */
    static boolean globMatch(String pattern, String name) {
        int star = pattern.indexOf('*');
        if (star < 0) {
            return pattern.equals(name);
        }
        String prefix = pattern.substring(0, star);
        String suffix = pattern.substring(star + 1);
        if (!name.startsWith(prefix) || !name.endsWith(suffix)) {
            return false;
        }
        return name.length() >= prefix.length() + suffix.length();
    }
}

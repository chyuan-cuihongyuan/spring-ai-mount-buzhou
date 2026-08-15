package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.metrics.BuzhouMetricsHolder;
import org.springframework.ai.chat.messages.ToolResponseMessage;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 工具结果尺寸防护（spec 31 / T110 / impl-85）：工具结果入模型上下文前的字符上限——
 * 超限截断（保留头部）+ 结构化提示尾；指标 {@code buzhou.tools.result-truncated}
 * （tag tool=工具名，有限集口径）。MCP server 万行查询/整页 fetch 场景的上下文护栏。
 *
 * <p><b>豁免</b>：per-tool 覆盖（glob 通配键，值 = 字符数或 -1 禁用）；默认豁免
 * {@code read_range}（spill 自治理——显式分页读取）。
 *
 * <p><b>不自动转 spill</b>：截断结果自含提示（模型可细化查询/分页重读）；自动 spill
 * 需工具结果与调用关联落盘，复杂度不抵收益（fog）。
 */
public final class ToolResultLimiter {

    public static final int DEFAULT_LIMIT_CHARS = 20_000;

    private static final String TRUNCATION_MARKER =
            "\n…[结果已截断：原始 %d 字符，超出上限 %d。请细化查询或分页读取所需部分]";

    private final int defaultLimit;
    private final Map<String, Integer> overrides;

    public ToolResultLimiter(int defaultLimit, Map<String, Integer> overrides) {
        this.defaultLimit = defaultLimit;
        this.overrides = overrides == null ? Map.of() : Map.copyOf(overrides);
    }

    /** 默认档：20K 字符 + read_range 豁免。 */
    public static ToolResultLimiter withDefaults() {
        Map<String, Integer> overrides = new LinkedHashMap<>();
        overrides.put("read_range", -1); // spill 自治理：显式分页读取
        return new ToolResultLimiter(DEFAULT_LIMIT_CHARS, overrides);
    }

    public static ToolResultLimiter disabled() {
        return new ToolResultLimiter(-1, Map.of());
    }

    public ToolResponseMessage.ToolResponse apply(ToolResponseMessage.ToolResponse response) {
        String data = response.responseData();
        if (data == null) {
            return response;
        }
        int limit = limitFor(response.name());
        if (limit < 0 || data.length() <= limit) {
            return response;
        }
        BuzhouMetricsHolder.metrics().counter("buzhou.tools.result-truncated",
                "tool", response.name());
        String truncated = data.substring(0, limit)
                + TRUNCATION_MARKER.formatted(data.length(), limit);
        return new ToolResponseMessage.ToolResponse(response.id(), response.name(), truncated);
    }

    /** per-tool 生效上限（观测/测试查询面）：glob 覆盖优先（首个命中），否则默认；-1 = 不限。 */
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

    /** 极简 glob：{@code *} 通配任意串（含空）；其余字面比对。 */
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

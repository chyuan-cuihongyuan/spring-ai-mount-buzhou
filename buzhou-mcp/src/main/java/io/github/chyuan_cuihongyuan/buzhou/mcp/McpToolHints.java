package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.modelcontextprotocol.spec.McpSchema;

import java.util.Map;

/**
 * MCP 工具自报注解的 buzhou 观测面快照（spec 600 / T851 / impl 453，借鉴
 * modelcontextprotocol/spec 工具 {@code annotations} 字段语义）。
 *
 * <p><b>定位 = 观测/审计，不是护栏裁决</b>：与 {@link McpClientRegistry#dangerousToolNames()}
 * 的既有决策一致——不信任 server 自报元数据，危险性由客户端风险分类裁决；注解仅用于
 * 目录可见性、审计画像与注解漂移信号（如 readOnlyHint 翻转本身即可疑）。
 *
 * <p>SDK 侧 {@code Boolean} 访问器可空：null 一律记 {@code false}、title 记空串
 * （MCP 规范未声明即未知，统一按 false 口径记录，相等性比较语义稳定）。
 *
 * @param title 工具人类可读标题（无则空串）
 * @param readOnlyHint server 自报「只读、无副作用环境修改」
 * @param destructiveHint server 自报「可能破坏性修改环境」
 * @param idempotentHint server 自报「重复调用同参数结果一致」
 * @param openWorldHint server 自报「与外部实体交互（人/网页/外部系统）」
 */
public record McpToolHints(String title, boolean readOnlyHint, boolean destructiveHint,
                           boolean idempotentHint, boolean openWorldHint) {

    private static final McpToolHints EMPTY = new McpToolHints("", false, false, false, false);

    /** 无注解工具的统一空画像（annotations 缺失/null）。 */
    public static McpToolHints empty() {
        return EMPTY;
    }

    /** 从 SDK {@link McpSchema.Tool} 映射；annotations 为 null 返回 {@link #empty()}。 */
    public static McpToolHints from(McpSchema.Tool tool) {
        McpSchema.ToolAnnotations annotations = tool.annotations();
        if (annotations == null) {
            return EMPTY;
        }
        return new McpToolHints(
                annotations.title() == null ? "" : annotations.title(),
                Boolean.TRUE.equals(annotations.readOnlyHint()),
                Boolean.TRUE.equals(annotations.destructiveHint()),
                Boolean.TRUE.equals(annotations.idempotentHint()),
                Boolean.TRUE.equals(annotations.openWorldHint()));
    }

    /** 空注解快照（连接无注解口径时统一返回）。 */
    public static Map<String, McpToolHints> emptyMap() {
        return Map.of();
    }
}

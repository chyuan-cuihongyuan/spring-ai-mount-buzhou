package io.github.chyuan_cuihongyuan.buzhou.guard.config;

/**
 * 危险工具清单条目（spec 07 HITL 配置模型）。
 *
 * @param name          工具名，支持通配（如 {@code mcp:prod_*}）；精确名覆盖通配
 * @param requiredState 授权标记 key 前缀（对应 state {@code auth.*} 命名空间）
 * @param hint          提示文案，可嵌 diff 文本；支持 {@code ${paramName}} 入参占位
 * @param confirmation  确认模型（标题 + 选项）
 */
public record DangerousToolEntry(String name, String requiredState, String hint, Confirmation confirmation) {

    public DangerousToolEntry {
        name = name == null ? "" : name;
        requiredState = requiredState == null ? "" : requiredState;
        hint = hint == null ? "" : hint;
    }
}

package io.github.chyuan_cuihongyuan.buzhou.core.testsupport;

/**
 * 脚本/录制回放中的一条工具调用规格（工具名 + JSON 入参）。
 *
 * @param name      工具名
 * @param arguments JSON 字符串入参（与真实 provider 的 tool-call arguments 同形）
 */
public record ToolCallSpec(String name, String arguments) {
}

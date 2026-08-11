package io.github.chyuan_cuihongyuan.buzhou.core.recovery;

import org.springframework.ai.chat.model.ToolContext;

/**
 * 工具幂等键业务覆盖提取器（spec「幂等三件套 ② 幂等键」/ CONTEXT「幂等键」）。
 *
 * <p>框架默认幂等键 = 会话内的 {@code toolCallId}（模型每次调用稳定 id，天然满足
 * 「会话 + 轮次 + 调用序号」唯一性）。<b>语义幂等</b>的业务工具（如「同订单号不重复下单」）
 * 可实现本接口，从工具入参取业务标识（如订单号）覆盖默认键，使去重按正确的业务语义命中。
 *
 * <p>实现方式：工具回调类同时实现 {@link org.springframework.ai.tool.ToolCallback} 与本接口。
 * 执行脊柱（{@code HarnessToolCallingManager.executeOne}）与恢复重放
 * （{@code DanglingCallRepairer}）在派生幂等键时，若工具是本接口实例则用业务键，否则走默认键。
 *
 * <p>未声明提取器（默认 no-op）→ 走框架默认键。本接口为 additive 扩展，既有工具源码 / 二进制兼容。
 * 去重作用域 = 会话内（跨会话重复防护不在框架职责内）。
 */
@FunctionalInterface
public interface IdempotencyKeyExtractor {

    /**
     * 从工具入参（JSON 串）派生业务幂等键。
     *
     * @param toolInput 工具入参（与 {@code ToolCallback.call} 收到的 JSON 串一致）
     * @param toolContext 工具上下文（恢复重放路径可能为 {@code null}）
     * @return 业务幂等键（不应为 {@code null}；返回 {@code null} 时回退框架默认键）
     */
    String extractKey(String toolInput, ToolContext toolContext);
}

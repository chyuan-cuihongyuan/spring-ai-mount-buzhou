package io.github.chyuan_cuihongyuan.buzhou.core.session;

/**
 * 运行时正在 drain（优雅停机）时拒新 {@link AgentRuntime#spawn} 的信号异常（spec「06 优雅停机 · 拒新」）。
 *
 * <p>drain 开始后 {@link AgentRuntime#spawn} 立即抛出本异常——不排队、不缓冲：
 * 拒绝即调用方的路由信号（向另一实例续接 / 重试 / 退避由调用方决策）。
 * 异常 message 带 sessionId 与「实例正在 drain」上下文，便于排障。
 */
public class RuntimeDrainingException extends RuntimeException {

    public RuntimeDrainingException(String sessionId) {
        super("Runtime is draining, cannot spawn session: " + sessionId);
    }
}

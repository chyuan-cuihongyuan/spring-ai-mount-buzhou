package io.github.chyuan_cuihongyuan.buzhou.core.session;

/**
 * 会话生命周期观察者：机制模块（buzhou-observability）实现并经
 * {@link SessionAssemblyContext#addObserver(SessionObserver)} 注册，
 * 由 {@code DefaultAgentSession} 在生命周期点回调，用于开/关 SESSION span、强制 flush 等。
 *
 * <p>回调在会话主线程同步执行；实现方应保持轻量（开 span 仅入队，不阻塞）。
 */
public interface SessionObserver {

    /** 会话装配完成、首次 chat 之前调用；用于开 SESSION span。 */
    default void onOpen() {
    }

    /** 每次 {@code chat()} 开始时调用（轮次序号已递增）。 */
    default void onTurnStart(int turnSeq, String userInput) {
    }

    /** 每次 {@code chat()} 完结时调用（最终回复已生成，turn.completed=true）。 */
    default void onTurnEnd(int turnSeq, String finalReply) {
    }

    /** 轮次异常终止时调用（如 stream 错误）；用于置在途 Turn span 为 ERROR 并关闭，防泄漏。 */
    default void onTurnError(int turnSeq, Throwable error) {
    }

    /** 会话关闭时调用；用于关 SESSION span + 强制 flush。 */
    default void onClose() {
    }

    /** 取消在途轮次时调用；用于置在途 Turn/ModelCall span 为 CANCELLED。 */
    default void onCancel() {
    }
}

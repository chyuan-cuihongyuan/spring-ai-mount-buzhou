package io.github.chyuan_cuihongyuan.buzhou.core.internal.session;

/**
 * spec 46 §B / T171 / impl-140：流式回复累计时长超限标记异常。
 *
 * <p>与相邻信号间隔 timeout（{@code Flux.timeout}，{@code TimeoutException}）正交：本异常标记
 * 「自订阅起整条流的累计时长」超限——持续慢滴流（每间隔内都有信号、永不触发间隔 timeout）的唯一
 * 硬顶。经由 {@code takeUntilOther(delay → error)} 语义以 onError 终结流，复用既有
 * {@code doOnError → failTurnOnce} 收尾链路（TURN 记账 / span 关闭 / 在途计数递减均既有语义），
 * 并按 {@code buzhou.stream.cancelled{reason=deadline}} 计数。
 */
class StreamTotalTimeoutException extends RuntimeException {

    StreamTotalTimeoutException(java.time.Duration cap) {
        super("流式回复累计时长超限（" + cap.toMillis() + "ms）：慢滴流防护截断——"
                + "请检查模型/网络吞吐或调大 buzhou.core.stream-total-timeout");
    }
}

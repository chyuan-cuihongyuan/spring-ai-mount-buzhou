package io.github.chyuan_cuihongyuan.buzhou.resilience;

import java.time.Duration;

/**
 * 模型调用超过 {@code deadline} 的终态失败（spec「统一超时」）。
 *
 * <p>由 {@code ResilienceAdvisor} 的 deadline 兜底抛出（{@code Future.get(deadline)} 超时后 {@code cancel(true)}
 * 中断在途调用）。重试回路将其视为<b>终态</b>——不重试、向上抛回 {@code HookAdvisor} 触发 {@code onModelError}。
 * 与 provider 自身的读超时（经分类器归 NETWORK、可重试）区分：本异常专指不周山施加的会话级 deadline。
 */
public class ModelCallTimeoutException extends RuntimeException {

    private final Duration deadline;

    public ModelCallTimeoutException(Duration deadline) {
        super("模型调用超时（deadline=" + deadline.toSeconds() + "s）");
        this.deadline = deadline;
    }

    public Duration deadline() {
        return deadline;
    }
}

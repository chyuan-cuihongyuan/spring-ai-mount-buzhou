package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.springframework.ai.chat.model.ToolContext;

import java.util.function.Supplier;

/**
 * 取消令牌（wayfinder2 impl-05 / T31）：随 {@link ToolContext} 下发给每个工具执行，
 * 长任务可<b>协作式</b>主动轮询 {@link #isCancelled()} 提前中止（无需依赖线程中断）——
 * 「取消 token 贯穿工具执行链」的落地载体。
 *
 * <p>令牌的取消态由 {@link HarnessToolCallingManager#requestCancel} 驱动
 * （{@link io.github.chyuan_cuihongyuan.buzhou.core.session.CancelMode#IMMEDIATE}
 * 及以后续档位立即置位）。
 */
public final class CancellationToken {

    /** ToolContext 中携带取消令牌的键。 */
    public static final String KEY = "buzhou.cancelToken";

    private final Supplier<Boolean> cancelled;

    private CancellationToken(Supplier<Boolean> cancelled) {
        this.cancelled = cancelled;
    }

    public static CancellationToken of(Supplier<Boolean> cancelled) {
        return new CancellationToken(cancelled);
    }

    /** 从 ToolContext 取当前令牌（未注入时返回恒 false 的空令牌，工具侧零判空）。 */
    public static CancellationToken from(ToolContext toolContext) {
        if (toolContext != null && toolContext.getContext() != null) {
            Object value = toolContext.getContext().get(KEY);
            if (value instanceof CancellationToken token) {
                return token;
            }
        }
        return of(() -> false);
    }

    /** 是否已请求取消。 */
    public boolean isCancelled() {
        return Boolean.TRUE.equals(cancelled.get());
    }
}

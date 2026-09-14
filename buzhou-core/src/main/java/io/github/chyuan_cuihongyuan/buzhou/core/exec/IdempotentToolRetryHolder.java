package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouTool;
import org.springframework.ai.tool.ToolCallback;

import java.util.Set;

/**
 * 幂等工具瞬断重试自动装配通道（spec 1511 / T2273，spec 05「运行期瞬断重试」
 * 承诺的装配收口——design-incompleteness F1 落地）：既有 {@link RetryingToolCallback}
 * 装饰器（spec 133 / B 会话 #95）此前靠宿主手动包装（幂等契约归声明方），本通道
 * 把它变成声明式——opt-in 开启后，@BuzhouTool.idempotent=true 或显式白名单内的
 * 工具在会话装配期自动包上重试（HookedToolCallback 内层：beforeTool/afterTool
 * hook 只见逻辑调用一次，重试是物理层）。
 *
 * <p>Holder 模式（EvalPrunePolicyHolder / RetryBudgetHolder 同款）：Spring 装配
 * 开启（{@code buzhou.core.tool-transient-retry.enabled=true}）；未装配 = null =
 * 装配不包（既有手动包装路径零变化）。
 *
 * @param policy   重试策略（透传既有 RetryPolicy）
 * @param idempotentOverrides 显式幂等白名单（无注解第三方工具入此集即自动包装）
 */
public record IdempotentToolRetryHolder(
        RetryingToolCallback.RetryPolicy policy, Set<String> idempotentOverrides) {

    public IdempotentToolRetryHolder {
        policy = policy == null ? RetryingToolCallback.RetryPolicy.defaults() : policy;
        idempotentOverrides = idempotentOverrides == null ? Set.of() : Set.copyOf(idempotentOverrides);
    }

    /**
     * 装配判定包装：Holder 未开启（null）或工具不幂等 → 原引用透传；
     * 幂等（@BuzhouTool.idempotent=true 或白名单）→ 包既有 RetryingToolCallback。
     */
    public ToolCallback wrapIfIdempotent(ToolCallback delegate) {
        BuzhouTool meta = delegate.getClass().getAnnotation(BuzhouTool.class);
        boolean idempotent = (meta != null && meta.idempotent())
                || idempotentOverrides.contains(delegate.getToolDefinition().name());
        return idempotent ? RetryingToolCallback.wrap(delegate, policy) : delegate;
    }

    /** 进程级 Holder（装配开启；未开启 null——HarnessAssembler 判空零包装）。 */
    public static final class Holder {
        private static volatile IdempotentToolRetryHolder current;

        private Holder() {
        }

        /** 开启（Spring 装配调用一次）。 */
        public static void enable(IdempotentToolRetryHolder holder) {
            current = holder;
        }

        /** 当前通道配置（未开启 = null）。 */
        public static IdempotentToolRetryHolder current() {
            return current;
        }

        /** 关闭（装配关闭钩子与测试隔离双用途）。 */
        public static void reset() {
            current = null;
        }
    }
}

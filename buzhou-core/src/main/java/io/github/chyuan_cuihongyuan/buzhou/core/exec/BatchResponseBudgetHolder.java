package io.github.chyuan_cuihongyuan.buzhou.core.exec;

/**
 * 批级工具结果回喂预算进程级 Holder（spec 1526 / T2303，Anthropic 工具结果
 * token 预算思想）：单工具限幅（ToolResultLimiter）之上的批内总量上限——
 * N 个工具各自限内但合计巨大的回喂仍会撑爆上下文。0 = 关（默认零行为）；
 * 超限时按响应长度降序贪心截大者（保留尽量多的小结果完整）。
 *
 * <p>Holder 模式（EvalPrunePolicyHolder / ToolResultLimiterHolder 同款）：
 * Spring 装配开启（{@code buzhou.core.tool-batch-response-budget} > 0 声明即启用）；
 * HarnessAssembler 构造期拾取注入 per-session manager。
 */
public final class BatchResponseBudgetHolder {

    private static volatile int budgetChars;

    private BatchResponseBudgetHolder() {
    }

    /** 开启（正数 = 批回喂总字符上限）。 */
    public static void enable(int budgetChars) {
        BatchResponseBudgetHolder.budgetChars = Math.max(0, budgetChars);
    }

    /** 当前预算（0 = 关）。 */
    public static int current() {
        return budgetChars;
    }

    /** 关闭（测试隔离用）。 */
    public static void reset() {
        budgetChars = 0;
    }
}

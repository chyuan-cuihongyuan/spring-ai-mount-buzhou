package io.github.chyuan_cuihongyuan.buzhou.core.session;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 有界 Turn 策略（wayfinder T17 / docs/spec/11 core + wayfinder2 impl-04 / docs/spec/12）：
 * 为 Turn 内 think→tool 递归引入可配置上界与<b>可组合停止条件</b>，超界走可插兜底 handler
 * 产出优雅最终回复——即便模型陷入工具调用死循环，成本与延迟也有硬上限。
 *
 * <p>停止条件建模为 {@code Predicate<TurnLoopContext>} 链（JDK Predicate 原生支持
 * {@code and(...)}/{@code or(...)} 组合），开箱提供「预算（轮数）」与「超时」两条内置条件；
 * 工具信号 / 外部取消等由接入方以自定义 Predicate 表达（如闭包引用取消旗标）。
 *
 * <p>impl-04 / T30 增 <b>{@code retryBudget}</b>：每 Turn 的「工具参数校验失败重试预算」
 * （与轮数上界独立扣减）——工具入参未过 schema 回喂校验反馈（REASK）累计超过预算时，
 * 循环优雅收尾（REASK_FAILED），防模型无限重提坏参数。
 *
 * <p>impl-28 / spec 13 §core-2 增 <b>{@code turnDeadline}</b>：整 Turn 硬预算（模型调用 +
 * 全部工具轮次），与 {@code loopTimeout} 打通——两者都配置时取<b>更紧者</b>作为有效
 * {@link TurnDeadline} 贯穿工具派发与外层 join（轮间停止条件裁决之外补上「轮内」截断，
 * 不响应中断的挂死工具也无法拖死会话）。仅配置 {@code loopTimeout} 时其语义同时升级为
 * 硬 Deadline（轮间停止条件保留不变）。
 *
 * <p>来源：Vercel {@code stopWhen} / OpenAI {@code max_turns} / AutoGen 可组合终止条件 +
 * Pydantic AI {@code retries}（默认 1）的 best-of-breed 思想。与工具侧「错误即反馈」
 * （{@code ToolErrorFeedback}）正交：那是单工具失败的恢复通道，这是整轮递归的成本护栏。
 *
 * @param maxToolRounds     每 Turn think→tool 递归上界（工具执行轮数）；{@code null} = 不设界
 * @param loopTimeout       工具递归循环整体耗时上界；{@code null} = 不限制
 * @param stopConditions    额外可组合停止条件（任一命中即停，在本轮工具执行<b>前</b>裁决）
 * @param gracefulFinalizer 超界兜底 handler（产出优雅最终回复文案）；{@code null} = 默认文案
 * @param retryBudget       每 Turn 参数校验失败重试预算（Pydantic AI 默认 retries=1~2 取 2）；
 *                          {@code null} = 框架默认 {@link #DEFAULT_RETRY_BUDGET}；由
 *                          {@code BoundedToolCallingAdvisor} 结合校验失败计数裁决
 * @param turnDeadline      impl-28：整 Turn 硬预算（含模型调用与全部工具轮次）；
 *                          {@code null} = 不设（与 {@code loopTimeout} 都为 null 时保持
 *                          既有无限等待行为）
 */
public record TurnLoopPolicy(
        Integer maxToolRounds,
        Duration loopTimeout,
        List<Predicate<TurnLoopContext>> stopConditions,
        Function<TurnLoopContext, String> gracefulFinalizer,
        Integer retryBudget,
        Duration turnDeadline) {

    /** 框架默认上界（业界保守值）：单 Turn 最多 40 个工具执行轮，防御 runaway 死循环。 */
    public static final int DEFAULT_MAX_TOOL_ROUNDS = 40;

    /** impl-04：框架默认参数校验重试预算（Pydantic AI 语义：允许 1~2 次自愈重试）。 */
    public static final int DEFAULT_RETRY_BUDGET = 2;

    public TurnLoopPolicy {
        stopConditions = stopConditions == null ? List.of() : List.copyOf(stopConditions);
    }

    /** 兼容 4 参构造（retryBudget 取框架默认、turnDeadline 不设）。 */
    public TurnLoopPolicy(Integer maxToolRounds, Duration loopTimeout,
                          List<Predicate<TurnLoopContext>> stopConditions,
                          Function<TurnLoopContext, String> gracefulFinalizer) {
        this(maxToolRounds, loopTimeout, stopConditions, gracefulFinalizer, null, null);
    }

    /** 兼容 5 参构造（impl-28 前的规范构造签名；turnDeadline 不设）。 */
    public TurnLoopPolicy(Integer maxToolRounds, Duration loopTimeout,
                          List<Predicate<TurnLoopContext>> stopConditions,
                          Function<TurnLoopContext, String> gracefulFinalizer,
                          Integer retryBudget) {
        this(maxToolRounds, loopTimeout, stopConditions, gracefulFinalizer, retryBudget, null);
    }

    /** 仅设轮数上界（最常用）。 */
    public static TurnLoopPolicy of(int maxToolRounds) {
        return new TurnLoopPolicy(maxToolRounds, null, List.of(), null, null);
    }

    /** 轮数上界 + 参数校验重试预算。 */
    public static TurnLoopPolicy of(int maxToolRounds, int retryBudget) {
        return new TurnLoopPolicy(maxToolRounds, null, List.of(), null, retryBudget);
    }

    /** 框架默认策略：40 轮上界、无超时、无自定义条件、校验重试预算 2。 */
    public static TurnLoopPolicy defaults() {
        return new TurnLoopPolicy(DEFAULT_MAX_TOOL_ROUNDS, null, List.of(), null, null);
    }

    /** 完全不设界（legacy 逃生舱；不建议长期使用）。 */
    public static TurnLoopPolicy unbounded() {
        return new TurnLoopPolicy(null, null, List.of(), null, null);
    }

    /** 生效的校验重试预算（null 归一为默认）。 */
    public int effectiveRetryBudget() {
        return retryBudget == null ? DEFAULT_RETRY_BUDGET : retryBudget;
    }

    /** 拷贝并设置整 Turn 硬预算（fluent 便捷入口，其余字段不变）。 */
    public TurnLoopPolicy withTurnDeadline(Duration budget) {
        return new TurnLoopPolicy(maxToolRounds, loopTimeout, stopConditions,
                gracefulFinalizer, retryBudget, budget);
    }

    /**
     * impl-28：有效 Turn 预算——{@code turnDeadline} 与 {@code loopTimeout} 都配置时取
     * <b>更紧者</b>（打通两者语义）；仅其一配置则用之；都为 null 返回 {@code null}
     * （不设界，保持既有无限等待行为）。
     */
    public Duration effectiveTurnBudget() {
        if (turnDeadline != null && loopTimeout != null) {
            return loopTimeout.compareTo(turnDeadline) <= 0 ? loopTimeout : turnDeadline;
        }
        return turnDeadline != null ? turnDeadline : loopTimeout;
    }

    /**
     * impl-28：有效 Turn Deadline 对象（{@link #effectiveTurnBudget()} 的对象化形式）；
     * 未配置预算时返回 {@link TurnDeadline#none()} 哨兵（等待方据此保持既有无限等行为）。
     */
    public TurnDeadline effectiveTurnDeadline() {
        Duration budget = effectiveTurnBudget();
        return budget == null ? TurnDeadline.none() : TurnDeadline.in(budget);
    }

    /** 内置停止条件：轮数预算。 */
    public static Predicate<TurnLoopContext> maxToolRounds(int max) {
        return ctx -> ctx.nextToolRound() > max;
    }

    /** 内置停止条件：循环超时。 */
    public static Predicate<TurnLoopContext> loopTimeout(Duration timeout) {
        return ctx -> !ctx.elapsed().minus(timeout).isNegative();
    }

    /** 即将执行第 {@code ctx.nextToolRound()} 轮工具前裁决：任一条件命中即应停止。 */
    public boolean shouldStop(TurnLoopContext ctx) {
        if (maxToolRounds != null && maxToolRounds(maxToolRounds).test(ctx)) {
            return true;
        }
        if (loopTimeout != null && loopTimeout(loopTimeout).test(ctx)) {
            return true;
        }
        for (Predicate<TurnLoopContext> condition : stopConditions) {
            if (condition.test(ctx)) {
                return true;
            }
        }
        return false;
    }

    /** 超界兜底文案（可插 handler；默认优雅收尾并如实告知用户）。 */
    public String gracefulFinal(TurnLoopContext ctx) {
        if (gracefulFinalizer != null) {
            return gracefulFinalizer.apply(ctx);
        }
        return "本轮任务已在预算内收尾：为控制成本与延迟，工具调用循环在 "
                + ctx.executedToolRounds() + " 轮后停止。"
                + "已完成的工具结果仍可用于回答；如需继续深入，请开启新一轮对话。";
    }

    /** 校验重试预算耗尽的兜底文案（REASK_FAILED 优雅收尾）。 */
    public String reaskFailedFinal(TurnLoopContext ctx, int validationFailures) {
        return "本轮任务已收尾：工具参数校验失败次数达到重试预算上限（" + validationFailures
                + " 次），为避免无效循环在此停止。请基于已有信息给出结论，"
                + "或向用户澄清所需参数后重新发起。";
    }
}

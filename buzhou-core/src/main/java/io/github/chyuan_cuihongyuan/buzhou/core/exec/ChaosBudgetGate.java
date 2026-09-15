package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import java.util.List;

/**
 * 混沌预算门（spec 1822 / T2845 / impl 1423）——Netflix Chaos Monkey /
 * Chaos Toolkit 稳态实验思想：混沌实验的价值依赖**有界的爆炸半径**——
 * 只在允许窗口内（如低峰时段）且预算未烧完时放行；窗口约束优先于预算
 *（安全第一：不在窗口内给多少钱都不跑）。预算口径（实验分钟数/次数）
 * 由调用方声明；Usage 读数回答预算烧到几成。
 *
 * <p>纯函数零状态、只裁决不执行（注入动作归宿主）。
 */
public final class ChaosBudgetGate {

    private ChaosBudgetGate() {
    }

    /** 放行三态：MAY_RUN 可跑 / OUT_OF_BUDGET 预算耗尽 / FORBIDDEN_WINDOW 窗口外。 */
    public enum Verdict {

        /** 窗口内且预算未尽——可注入。 */
        MAY_RUN,

        /** 预算耗尽——本周期不再实验。 */
        OUT_OF_BUDGET,

        /** 不在允许窗口（安全优先于预算）。 */
        FORBIDDEN_WINDOW
    }

    /** 允许窗口契约：start ≤ end（空窗口 start==end 合法——永不放行）。 */
    public record Window(long windowStartMillis, long windowEndMillis) {

        public Window {
            if (windowStartMillis > windowEndMillis) {
                throw new IllegalArgumentException(
                        "非法窗口：start=" + windowStartMillis + " > end=" + windowEndMillis);
            }
        }

        boolean contains(long nowMillis) {
            return nowMillis >= windowStartMillis && nowMillis <= windowEndMillis;
        }
    }

    /**
     * 放行裁决。契约：budgetRemainingMillis ≥ 0（fail-fast）；语义：窗口
     * 优先（不在窗即 FORBIDDEN——预算再多不跑），窗内看预算（0 即耗尽）。
     */
    public static Verdict decide(long budgetRemainingMillis, long nowMillis, Window window) {
        if (budgetRemainingMillis < 0) {
            throw new IllegalArgumentException(
                    "budgetRemainingMillis 不能为负：" + budgetRemainingMillis);
        }
        if (!window.contains(nowMillis)) {
            return Verdict.FORBIDDEN_WINDOW;
        }
        return budgetRemainingMillis == 0 ? Verdict.OUT_OF_BUDGET : Verdict.MAY_RUN;
    }

    /**
     * 预算使用账。null 按空表；超支实验照实入账（remaining 钳 0——诚实但
     * 不外泄负值）。
     */
    public static Usage usage(long budgetMillis, List<Long> experimentSpentMillis) {
        if (budgetMillis < 0) {
            throw new IllegalArgumentException("budgetMillis 不能为负：" + budgetMillis);
        }
        List<Long> window = experimentSpentMillis == null ? List.of() : experimentSpentMillis;
        long spent = 0;
        for (Long v : window) {
            if (v == null || v < 0) {
                throw new IllegalArgumentException("实验花费不能为 null 或负");
            }
            spent += v;
        }
        return new Usage(budgetMillis, spent, Math.max(0, budgetMillis - spent));
    }

    /** @param remaining 剩余预算（钳 0）；burnRatio 烧尽比（无预算 -1 哨兵） */
    public record Usage(long budgetMillis, long spentMillis, long remainingMillis) {

        public double burnRatio() {
            return budgetMillis == 0 ? -1d : (double) spentMillis / budgetMillis;
        }

        public boolean exhausted() {
            return remainingMillis == 0;
        }
    }
}

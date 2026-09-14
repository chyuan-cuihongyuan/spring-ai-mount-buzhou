package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.GradientAdaptiveLimiter;

/**
 * 梯度限流器进程级 Holder（spec 1639 / T2429，spec 1617 装配面）：
 * 工具批耗时经此喂入（观测先行——View 读数给运维看全局工具路径的延迟梯度，
 * tryAcquire 闸接入待数据积累后独立裁决）。默认 min=4/max=64/tolerance=0.2。
 */
public final class GradientLimiterHolder {

    private static volatile GradientAdaptiveLimiter limiter =
            new GradientAdaptiveLimiter(GradientAdaptiveLimiter.Config.defaults());

    private GradientLimiterHolder() {
    }

    /** 替换实例（测试）。 */
    public static void install(GradientAdaptiveLimiter instance) {
        limiter = instance == null
                ? new GradientAdaptiveLimiter(GradientAdaptiveLimiter.Config.defaults()) : instance;
    }

    /** 当前限流器。 */
    public static GradientAdaptiveLimiter limiter() {
        return limiter;
    }

    /** 观测快照便捷面。 */
    public static GradientAdaptiveLimiter.View view() {
        return limiter().view();
    }
}

package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.GradientAdaptiveLimiter;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 梯度限流器观测接线测试（spec 1639 / T2429–T2430 / impl 1192）：
 * 批耗时喂入经 Holder（直接喂面——executeToolCalls 的集成需完整会话链，
 * 数据面语义与此等价）；View 读数可观测。
 */
class GradientLimiterWiringTest {

    @Test
    void holderFeedsAndExposesView() {
        GradientLimiterHolder.install(null); // 重置默认
        GradientLimiterHolder.limiter().record(100);
        GradientLimiterHolder.limiter().record(100);
        GradientAdaptiveLimiter.View view = GradientLimiterHolder.view();
        assertThat(view.baselineEmaMillis()).isEqualTo(100.0);
        assertThat(view.recentEmaMillis()).isEqualTo(100.0);
        assertThat(view.gradient()).isEqualTo(1.0);
    }

    @Test
    void installReplacesInstance() {
        GradientAdaptiveLimiter fresh = new GradientAdaptiveLimiter(
                new GradientAdaptiveLimiter.Config(2, 8, 0.3));
        GradientLimiterHolder.install(fresh);
        assertThat(GradientLimiterHolder.limiter()).isSameAs(fresh);
        GradientLimiterHolder.install(null);
        assertThat(GradientLimiterHolder.view().limit()).isEqualTo(4); // 默认 min 起步
    }
}

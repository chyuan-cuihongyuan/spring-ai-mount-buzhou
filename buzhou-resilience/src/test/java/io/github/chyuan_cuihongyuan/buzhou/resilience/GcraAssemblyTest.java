package io.github.chyuan_cuihongyuan.buzhou.resilience;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.session.RuntimeConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.testsupport.ScriptedChatModel;
import io.github.chyuan_cuihongyuan.buzhou.resilience.config.ResilienceProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * GCRA yml 装配测试（spec 614 / T878–T879 / impl 467）：smoothing=gcra 声明即装配
 * GcraRateLimitBackend、默认/显式 token-bucket 走令牌桶缺省、smoothing 词汇闭集
 * 校验 fail-fast、GCRA 装配下 chat 管线端到端跑通。
 */
class GcraAssemblyTest {

    private static ResilienceProperties propsWithRateLimit(ResilienceProperties.RateLimit rl) {
        return new ResilienceProperties(true, 1,
                Duration.ofMillis(1), Duration.ofMillis(10), 2.0, 0.0, null, null,
                rl, null, null, null, null, null, null);
    }

    /** 默认（无 smoothing）与显式 token-bucket → 令牌桶缺省口径。 */
    @Test
    void defaultSmoothingKeepsTokenBucket() {
        ResilienceProperties.RateLimit tokenBucket = new ResilienceProperties.RateLimit(
                60, 1000, Duration.ofSeconds(1), null);
        assertThat(tokenBucket.isGcraSmoothing()).isFalse();
        ResilienceProperties.RateLimit explicit = new ResilienceProperties.RateLimit(
                60, 1000, Duration.ofSeconds(1), null, "token-bucket", null);
        assertThat(explicit.isGcraSmoothing()).isFalse();
        assertThat(ResilienceModule.configure(propsWithRateLimit(tokenBucket))).isNotNull();
    }

    /** smoothing 词汇闭集：未知值装配期 fail-fast（拼写错不静默宽容）。 */
    @Test
    void unknownSmoothingRejected() {
        ResilienceProperties.RateLimit bad = new ResilienceProperties.RateLimit(
                60, 1000, Duration.ofSeconds(1), null, "smooth", null);
        assertThatThrownBy(() -> ResilienceModule.configure(propsWithRateLimit(bad)))
                .isInstanceOf(io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouConfigurationException.class)
                .hasMessageContaining("smoothing");
    }

    /** E2E：smoothing=gcra + 宽裕配额 + FAIL_FAST 下 chat 跑通（GCRA 后端放行首个请求）。 */
    @Test
    void gcraSmoothingPassesChatPipeline() {
        ScriptedChatModel model = new ScriptedChatModel();
        model.enqueueText("ok");
        BuzhouStores stores = Buzhou.inMemoryStores();
        ResilienceProperties.RateLimit rl = new ResilienceProperties.RateLimit(
                600, 100000, Duration.ofMillis(100), "FAIL_FAST", "gcra", Duration.ofSeconds(30));
        var runtime = Buzhou.runtime(model, stores,
                ResilienceModule.configure(propsWithRateLimit(rl)));

        var session = runtime.spawn("app", "agent", "s-gcra");
        assertThat(session.chat("q")).isEqualTo("ok");
        session.close();
    }
}

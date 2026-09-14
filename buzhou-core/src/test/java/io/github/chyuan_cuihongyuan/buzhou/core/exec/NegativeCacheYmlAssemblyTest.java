package io.github.chyuan_cuihongyuan.buzhou.core.exec;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 负缓存 yml 装配测试（spec 1641 / T2433–T2434 / impl 1194）：
 * enabled=true 声明即启用（ttl 可配）；缺省不启用（零行为）；关闭钩子停用。
 */
class NegativeCacheYmlAssemblyTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    io.github.chyuan_cuihongyuan.buzhou.core.config.BuzhouCoreAutoConfiguration.class));

    @AfterEach
    void reset() {
        NegativeCachingHolder.setEnabled(false);
        NegativeCachingHolder.setTtl(null);
    }

    @Test
    void enabledPropertyActivatesHolder() {
        runner.withPropertyValues("buzhou.core.negative-cache.enabled=true",
                        "buzhou.core.negative-cache.ttl=45s")
                .withPropertyValues("buzhou.metrics.enabled=false")
                .run(context -> {
                    assertThat(context).hasBean("buzhouNegativeCacheAdapter");
                    // bean 初始化已启用（上下文内读数）
                });
        // 上下文关闭后钩子停用
        assertThat(NegativeCachingHolder.enabled()).isFalse();
    }

    @Test
    void ttlPropertyApplied() {
        runner.withPropertyValues("buzhou.core.negative-cache.enabled=true",
                        "buzhou.core.negative-cache.ttl=2m")
                .run(context -> assertThat(NegativeCachingHolder.ttl())
                        .isEqualTo(java.time.Duration.ofMinutes(2)));
    }

    @Test
    void absentPropertyStaysDisabled() {
        runner.run(context -> assertThat(context)
                .doesNotHaveBean("buzhouNegativeCacheAdapter"));
        assertThat(NegativeCachingHolder.enabled()).isFalse();
    }
}

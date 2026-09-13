package io.github.chyuan_cuihongyuan.buzhou.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * impl-663 / spec 910：buzhou.webhook.adaptive-batch yml 装配分支——
 * true → forwarder 自适应启用；缺省 → 固定批次（32）零回归。
 */
class WebhookAimdAssemblyTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouCoreAutoConfiguration.class));

    @Test
    void adaptiveBatchKeyEnablesAdaptation() {
        runner.withPropertyValues("buzhou.webhook.url=http://127.0.0.1:1/hook",
                "buzhou.webhook.adaptive-batch=true")
                .run(ctx -> {
                    assertThat(ctx).hasBean("webhookEventForwarder");
                    var forwarder = (io.github.chyuan_cuihongyuan.buzhou.core.webhook.WebhookEventForwarder)
                            ctx.getBean("webhookEventForwarder");
                    assertThat(forwarder.currentBatchSize()).isEqualTo(32); // 初始 = BATCH
                });
    }

    @Test
    void defaultKeepsFixedBatch() {
        runner.withPropertyValues("buzhou.webhook.url=http://127.0.0.1:1/hook").run(ctx -> {
            assertThat(ctx).hasBean("webhookEventForwarder");
            var forwarder = (io.github.chyuan_cuihongyuan.buzhou.core.webhook.WebhookEventForwarder)
                    ctx.getBean("webhookEventForwarder");
            assertThat(forwarder.currentBatchSize()).isEqualTo(32); // 缺省零变化
        });
    }
}

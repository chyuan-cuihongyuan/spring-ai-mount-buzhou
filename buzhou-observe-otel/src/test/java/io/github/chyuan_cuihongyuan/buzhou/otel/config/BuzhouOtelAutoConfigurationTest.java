package io.github.chyuan_cuihongyuan.buzhou.otel.config;

import io.github.chyuan_cuihongyuan.buzhou.observability.pipeline.PipelineSink;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class BuzhouOtelAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouOtelAutoConfiguration.class));

    @Test
    void disabledByDefault() {
        runner.run(ctx -> assertThat(ctx).doesNotHaveBean(PipelineSink.class));
    }

    @Test
    void enabledProducesSink() {
        // otlp 形态：构造时不触网，仅建 SDK + exporter；导出发生在 span 落地时
        runner.withPropertyValues("buzhou.observe.otel.enabled=true")
                .run(ctx -> assertThat(ctx).hasSingleBean(PipelineSink.class));
    }
}

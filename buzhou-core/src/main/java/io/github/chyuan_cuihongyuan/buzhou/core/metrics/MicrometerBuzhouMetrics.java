package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * micrometer 实现的 {@link BuzhouMetrics}（impl-41 / spec 13 §T66；micrometer 为
 * <b>optional</b> 依赖——本类仅在有 micrometer 的运行时经装配层条件加载）。
 * 命名 {@code buzhou.<mech>.<测量>}、tag 值有界；严禁 sessionId 进 tag（库内调用纪律）。
 */
public final class MicrometerBuzhouMetrics implements BuzhouMetrics {

    private final MeterRegistry registry;

    public MicrometerBuzhouMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    @Override
    public void counter(String name, long delta, String... tagKeyValue) {
        if (delta != 0) {
            registry.counter(name, tags(tagKeyValue)).increment(delta);
        }
    }

    @Override
    public void timer(String name, Duration duration, String... tagKeyValue) {
        Timer.builder(name).tags(tags(tagKeyValue))
                .register(registry)
                .record(duration);
    }

    @Override
    public void gauge(String name, Supplier<Number> value, String... tagKeyValue) {
        io.micrometer.core.instrument.Gauge
                .builder(name, value, supplier -> supplier.get().doubleValue())
                .tags(tags(tagKeyValue))
                .strongReference(true) // Supplier 由业务侧持有；不依赖弱引用语义
                .register(registry);
    }

    private static Tags tags(String... tagKeyValue) {
        if (tagKeyValue == null || tagKeyValue.length == 0) {
            return Tags.empty();
        }
        if (tagKeyValue.length % 2 != 0) {
            throw new IllegalArgumentException("tag 须为 key/value 成对（收到 "
                    + java.util.Arrays.toString(tagKeyValue) + "）");
        }
        Tag[] tags = new Tag[tagKeyValue.length / 2];
        for (int i = 0; i < tags.length; i++) {
            tags[i] = Tag.of(tagKeyValue[2 * i], tagKeyValue[2 * i + 1]);
        }
        return Tags.of(tags);
    }
}

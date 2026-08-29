package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 132 §B / T457：tag 基数守卫红队——封顶内直通原值；越限折 __overflow__
 * （样本不丢、维度细分丢）；既有值在折后仍直通；指标名空间满全折；畸形键值对
 * 透传不放大故障；timer 面同守卫。借鉴：Loki label cardinality limit。
 */
class TagCardinalityGuardTest {

    /** 记录型委托：捕获 (name, tags) 面。 */
    private static final class Recording implements BuzhouMetrics {
        final List<String> events = new CopyOnWriteArrayList<>();
        final Map<String, String[]> lastTagsByEvent = new ConcurrentHashMap<>();

        @Override
        public void counter(String name, long delta, String... tagKeyValue) {
            events.add("counter:" + name);
            lastTagsByEvent.put(name, tagKeyValue);
        }

        @Override
        public void timer(String name, Duration duration, String... tagKeyValue) {
            events.add("timer:" + name);
            lastTagsByEvent.put(name, tagKeyValue);
        }
    }

    @Test
    void withinCapPassesThroughUnchanged() {
        Recording sink = new Recording();
        TagCardinalityGuard guard = TagCardinalityGuard.wrap(sink, 3);

        guard.counter("buzhou.demo.outcome", 1, "outcome", "ok");
        guard.counter("buzhou.demo.outcome", 1, "outcome", "degraded");
        guard.counter("buzhou.demo.outcome", 1, "outcome", "ok"); // 在册重复直通

        assertThat(sink.lastTagsByEvent.get("buzhou.demo.outcome"))
                .containsExactly("outcome", "ok");
        assertThat(guard.folds()).isZero();
    }

    @Test
    void overCapFoldsToOverflowButEstablishedValuesKeepPassing() {
        Recording sink = new Recording();
        TagCardinalityGuard guard = TagCardinalityGuard.wrap(sink, 2);

        guard.counter("buzhou.demo.agent", 1, "agent", "alpha");
        guard.counter("buzhou.demo.agent", 1, "agent", "beta");
        guard.counter("buzhou.demo.agent", 1, "agent", "gamma"); // 越限折入

        assertThat(sink.lastTagsByEvent.get("buzhou.demo.agent"))
                .containsExactly("agent", "__overflow__");
        assertThat(guard.folds()).isEqualTo(1);

        // 既有值照常直通（折入只挡新值）
        guard.counter("buzhou.demo.agent", 1, "agent", "alpha");
        assertThat(sink.lastTagsByEvent.get("buzhou.demo.agent"))
                .containsExactly("agent", "alpha");
    }

    @Test
    void multipleTagsGuardedIndependently() {
        Recording sink = new Recording();
        TagCardinalityGuard guard = TagCardinalityGuard.wrap(sink, 1);

        guard.counter("buzhou.demo.x", 1, "outcome", "ok", "region", "cn");
        guard.counter("buzhou.demo.x", 1, "outcome", "failed", "region", "cn");
        guard.counter("buzhou.demo.x", 1, "outcome", "failed", "region", "us");

        // outcome 折（第二个值），region 各键独立也折；被折值不入册——第三次
        // 重见 failed 仍走折路径（folds=3：failed×2 + us×1）
        assertThat(sink.lastTagsByEvent.get("buzhou.demo.x"))
                .containsExactly("outcome", "__overflow__", "region", "__overflow__");
        assertThat(guard.folds()).isEqualTo(3);
    }

    @Test
    void nameSpaceFullFoldsWholeNewMetric() {
        Recording sink = new Recording();
        TagCardinalityGuard guard = TagCardinalityGuard.wrap(sink, 4);
        for (int i = 0; i < TagCardinalityGuard.MAX_NAMES; i++) {
            guard.counter("buzhou.n" + i, 1, "k", "v");
        }
        guard.counter("buzhou.one-more", 1, "k", "fresh");
        assertThat(sink.lastTagsByEvent.get("buzhou.one-more"))
                .containsExactly("k", "__overflow__");
        assertThat(guard.folds()).isEqualTo(1);
        // 既有名不受影响
        guard.counter("buzhou.n0", 1, "k", "v2");
        assertThat(sink.lastTagsByEvent.get("buzhou.n0")).containsExactly("k", "v2");
    }

    @Test
    void malformedPairsPassThroughWithoutFailing() {
        Recording sink = new Recording();
        TagCardinalityGuard guard = TagCardinalityGuard.wrap(sink);
        guard.counter("buzhou.demo.malformed", 1, "lonely-key");
        assertThat(sink.lastTagsByEvent.get("buzhou.demo.malformed"))
                .containsExactly("lonely-key");
        guard.timer("buzhou.demo.malformed", Duration.ofMillis(5));
        assertThat(sink.events).contains("timer:buzhou.demo.malformed");
        assertThat(guard.folds()).isZero();
    }

    @Test
    void timerPathFoldsLikeCounter() {
        Recording sink = new Recording();
        TagCardinalityGuard guard = TagCardinalityGuard.wrap(sink, 1);
        guard.timer("buzhou.demo.latency", Duration.ofMillis(1), "bucket", "fast");
        guard.timer("buzhou.demo.latency", Duration.ofMillis(2), "bucket", "slow");
        assertThat(sink.lastTagsByEvent.get("buzhou.demo.latency"))
                .containsExactly("bucket", "__overflow__");
    }

    @Test
    void wrapValidatesCap() {
        assertThatThrownBy(() -> TagCardinalityGuard.wrap(BuzhouMetrics.noop(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 203 / T576：事件去重回归——重复拦 / 异质过 / 环形滚出 / Map 序等价 /
 * 透传组合 / 校验。
 */
class EventDeduplicatorTest {

    private static final class Collector implements
            io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener {
        final CopyOnWriteArrayList<SessionEvent> received = new CopyOnWriteArrayList<>();

        @Override
        public void onEvent(SessionEvent event) {
            received.add(event);
        }
    }

    @Test
    void exactDuplicateIsSuppressed() {
        Collector sink = new Collector();
        EventDeduplicator dedup = new EventDeduplicator(sink);

        dedup.onEvent(SessionEvent.of("user.turn.completed", Map.of("sessionId", "s1")));
        dedup.onEvent(SessionEvent.of("user.turn.completed", Map.of("sessionId", "s1"))); // 完全相同

        assertThat(sink.received).hasSize(1);
    }

    @Test
    void heterogeneousEventsPassThrough() {
        Collector sink = new Collector();
        EventDeduplicator dedup = new EventDeduplicator(sink);

        dedup.onEvent(SessionEvent.of("a", Map.of("x", 1)));
        dedup.onEvent(SessionEvent.of("b", Map.of("x", 1)));  // 同 payload 异 type
        dedup.onEvent(SessionEvent.of("a", Map.of("x", 2)));  // 同 type 异 payload

        assertThat(sink.received).hasSize(3);
    }

    @Test
    void ringEvictionAllowsOldEventAgain() {
        Collector sink = new Collector();
        EventDeduplicator dedup = new EventDeduplicator(sink, 2);

        dedup.onEvent(SessionEvent.of("t", Map.of("k", "old")));
        dedup.onEvent(SessionEvent.of("t", Map.of("k", "1")));
        dedup.onEvent(SessionEvent.of("t", Map.of("k", "2"))); // old 滚出
        dedup.onEvent(SessionEvent.of("t", Map.of("k", "old"))); // 再过（窗语义）

        assertThat(sink.received).hasSize(4); // old 两次都过
    }

    @Test
    void mapOrderingDoesNotAffectFingerprint() {
        Collector sink = new Collector();
        EventDeduplicator dedup = new EventDeduplicator(sink);

        Map<String, Object> first = new LinkedHashMap<>();
        first.put("a", 1);
        first.put("b", 2);
        Map<String, Object> second = new LinkedHashMap<>();
        second.put("b", 2);
        second.put("a", 1);

        dedup.onEvent(SessionEvent.of("t", first));
        dedup.onEvent(SessionEvent.of("t", second)); // 键排序后等价——同指纹拦

        assertThat(sink.received).hasSize(1);
    }

    @Test
    void dedupedCountGrowsButForwardedStaysClean() {
        Collector sink = new Collector();
        EventDeduplicator dedup = new EventDeduplicator(sink);
        SessionEvent repeated = SessionEvent.of("t", Map.of("k", "v"));

        for (int i = 0; i < 5; i++) {
            dedup.onEvent(repeated);
        }

        assertThat(sink.received).hasSize(1); // 五发一达
        List<SessionEvent> single = sink.received;
        assertThat(single.getFirst().payload()).containsEntry("k", "v"); // 保真透传
    }

    @Test
    void argumentsValidated() {
        assertThatThrownBy(() -> new EventDeduplicator(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new EventDeduplicator(new Collector(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

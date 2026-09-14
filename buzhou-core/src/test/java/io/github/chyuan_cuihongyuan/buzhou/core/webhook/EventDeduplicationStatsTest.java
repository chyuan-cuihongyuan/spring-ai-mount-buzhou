package io.github.chyuan_cuihongyuan.buzhou.core.webhook;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEventListener;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1448 / T2196：去重聚合读面——passed/deduped 计数与守恒、去重率派生、
 * 环占用与容量、reset 只清计数不清环。
 */
class EventDeduplicationStatsTest {

    private static SessionEvent event(String type, String marker) {
        return SessionEvent.of(type, Map.of("marker", marker));
    }

    @Test
    void passAndDedupeCountedWithRatio() {
        CopyOnWriteArrayList<SessionEvent> seen = new CopyOnWriteArrayList<>();
        EventDeduplicator dedup = new EventDeduplicator(seen::add, 64);
        dedup.onEvent(event("tick", "first"));  // 放行
        dedup.onEvent(event("tick", "first"));  // 同指纹 → 去重
        dedup.onEvent(event("tick", "second")); // 放行
        var s = dedup.deduplicationStats();
        assertThat(s.passed()).isEqualTo(2);
        assertThat(s.deduped()).isEqualTo(1);
        assertThat(s.ringSize()).isEqualTo(2);
        assertThat(s.capacity()).isEqualTo(64);
        assertThat(s.deduplicationRatio()).isEqualTo(1.0d / 3);
        // delegate 只收到放行者
        assertThat(seen).hasSize(2);
    }

    @Test
    void freshDeduplicatorYieldsSentinel() {
        EventDeduplicator dedup = new EventDeduplicator(e -> {
        }, 32);
        assertThat(dedup.deduplicationStats().deduplicationRatio()).isEqualTo(-1d);
        assertThat(dedup.deduplicationStats().ringSize()).isZero();
    }

    @Test
    void resetClearsCountersOnly() {
        EventDeduplicator dedup = new EventDeduplicator(e -> {
        }, 32);
        dedup.onEvent(event("tick", "first"));
        dedup.onEvent(event("tick", "first"));
        dedup.resetStatsForTest();
        var s = dedup.deduplicationStats();
        assertThat(s.passed()).isZero();
        assertThat(s.deduped()).isZero();
        // 环/成员状态保留：同指纹再入仍被去重（计数从头累计）
        dedup.onEvent(event("tick", "first"));
        assertThat(dedup.deduplicationStats().deduped()).isEqualTo(1);
    }
}

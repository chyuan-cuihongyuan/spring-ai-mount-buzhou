package io.github.chyuan_cuihongyuan.buzhou.core.session;

import io.github.chyuan_cuihongyuan.buzhou.core.health.BuzhouSessionsEndpoint;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 415 §Testing / T721–T722：黏性路由——键确定性/分散；桶位界内确定性；
 * 面板 affinity 段（行携带键与桶、无索引诚实降级）。
 */
class SessionAffinityTest {

    @Test
    void shouldProduceDeterministicWellSpreadKeysAndBuckets() {
        String k1 = SessionAffinity.key("app", "s1");
        assertThat(k1).hasSize(8).matches("[0-9a-f]{8}");
        assertThat(SessionAffinity.key("app", "s1")).isEqualTo(k1); // 确定性
        assertThat(SessionAffinity.key("app2", "s1")).isNotEqualTo(k1); // appId 参与键
        assertThat(SessionAffinity.key("app", "s2")).isNotEqualTo(k1); // sessionId 参与键

        // 分散抽查：100 会话键无碰撞（8 hex=4G 空间，100 内碰撞概率可忽略）
        Set<String> keys = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            keys.add(SessionAffinity.key("app", "s-" + i));
        }
        assertThat(keys).hasSize(100);

        // 桶位：界内 + 确定性 + 8 桶下 100 会话铺满多数桶（无严重偏斜）
        Set<Integer> used = new HashSet<>();
        for (int i = 0; i < 100; i++) {
            int b = SessionAffinity.bucket("app", "s-" + i, 8);
            assertThat(b).isBetween(0, 7);
            assertThat(SessionAffinity.bucket("app", "s-" + i, 8)).isEqualTo(b);
            used.add(b);
        }
        assertThat(used.size()).isGreaterThanOrEqualTo(6);

        assertThatThrownBy(() -> SessionAffinity.bucket("a", "s", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 最小索引桩：只回一行 ACTIVE。 */
    private static final class StubIndex implements SessionIndexStore {
        @Override public void upsert(SessionInfo info) { }

        @Override public void delete(String sessionId) { }

        @Override public Optional<SessionInfo> get(String sessionId) {
            return Optional.empty();
        }

        @Override public List<SessionInfo> list(SessionIndexQuery query) {
            return List.of(new SessionInfo("s-1", "app-1", "agent",
                    SessionInfo.STATUS_ACTIVE, 0, 0, 3, Map.of()));
        }
    }

    @Test
    void shouldExposeAffinitySectionThroughSessionsEndpoint() {
        BuzhouSessionsEndpoint endpoint = new BuzhouSessionsEndpoint(
                new StubIndex(), null, null, 8);
        Map<String, Object> payload = endpoint.sessionsDashboard();
        @SuppressWarnings("unchecked")
        Map<String, Object> affinity = (Map<String, Object>) payload.get("affinity");
        assertThat(affinity).containsEntry("available", true).containsEntry("buckets", 8);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) affinity.get("rows");
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0)).containsEntry("sessionId", "s-1")
                .containsEntry("appId", "app-1")
                .containsEntry("affinityKey", SessionAffinity.key("app-1", "s-1"))
                .containsEntry("affinityBucket", SessionAffinity.bucket("app-1", "s-1", 8));

        // 无索引部署：诚实降级
        BuzhouSessionsEndpoint noIndex = new BuzhouSessionsEndpoint(null, null, null);
        @SuppressWarnings("unchecked")
        Map<String, Object> degraded = (Map<String, Object>) noIndex.sessionsDashboard()
                .get("affinity");
        assertThat(degraded).containsEntry("available", false);
    }
}

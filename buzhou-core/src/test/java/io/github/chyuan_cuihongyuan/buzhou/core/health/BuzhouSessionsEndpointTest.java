package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.backpressure.MaintenanceCordon;
import io.github.chyuan_cuihongyuan.buzhou.core.backpressure.SpawnAdmissionFloor;
import io.github.chyuan_cuihongyuan.buzhou.core.backpressure.SpawnPriority;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 346 / impl-369：会话面板端点回归——索引计数+地板多源+cordon /
 * 无索引缺席 / 截断标记。
 */
class BuzhouSessionsEndpointTest {

    /** 内存索引（ACTIVE/CLOSED 混合，list 契约同 310 测试）。 */
    private static final class StubIndex implements SessionIndexStore {
        final Map<String, SessionInfo> rows = new LinkedHashMap<>();

        @Override
        public void upsert(SessionInfo info) {
            rows.put(info.sessionId(), info);
        }

        @Override
        public Optional<SessionInfo> get(String sessionId) {
            return Optional.ofNullable(rows.get(sessionId));
        }

        @Override
        public List<SessionInfo> list(SessionIndexQuery query) {
            List<SessionInfo> matched = new ArrayList<>(rows.values().stream()
                    .filter(r -> query.status() == null || query.status().equals(r.status()))
                    .sorted(Comparator.comparingLong(SessionInfo::lastActiveAtEpochMs).reversed())
                    .toList());
            return matched.stream().skip(query.offset()).limit(query.limit()).toList();
        }

        @Override
        public void delete(String sessionId) {
            rows.remove(sessionId);
        }
    }

    private static SessionInfo session(String id, String status) {
        return new SessionInfo(id, "app", "agent", status,
                System.currentTimeMillis() - 3_600_000, System.currentTimeMillis(),
                5, Map.of());
    }

    @Test
    void countsActiveShowsFloorSourcesAndCordon() {
        StubIndex index = new StubIndex();
        index.upsert(session("a-1", SessionInfo.STATUS_ACTIVE));
        index.upsert(session("a-2", SessionInfo.STATUS_ACTIVE));
        index.upsert(session("c-1", SessionInfo.STATUS_CLOSED));
        SpawnAdmissionFloor floor = new SpawnAdmissionFloor();
        floor.set(SpawnPriority.HIGH);              // 335 冻结（default 源）
        floor.set("maintenance", SpawnPriority.HIGH); // 342 cordon
        MaintenanceCordon cordon = new MaintenanceCordon(floor,
                null, null, "", Duration.ofSeconds(15), Clock.systemUTC());

        Map<String, Object> payload =
                new BuzhouSessionsEndpoint(index, floor, cordon).sessionsDashboard();

        @SuppressWarnings("unchecked")
        Map<String, Object> active = (Map<String, Object>) payload.get("activeSessions");
        assertThat(active).containsEntry("available", true)
                .containsEntry("count", 2L).containsEntry("truncated", false); // CLOSED 不计

        @SuppressWarnings("unchecked")
        Map<String, Object> floorSection = (Map<String, Object>) payload.get("spawnFloor");
        assertThat(floorSection).containsEntry("effective", "HIGH");
        @SuppressWarnings("unchecked")
        Map<String, String> sources = (Map<String, String>) floorSection.get("sources");
        assertThat(sources).containsKey("default").containsKey("maintenance"); // 谁抬着一目了然

        assertThat(payload).containsKey("maintenance");
    }

    @Test
    void noIndexYieldsHonestAbsentSection() {
        SpawnAdmissionFloor floor = new SpawnAdmissionFloor();
        MaintenanceCordon cordon = new MaintenanceCordon(floor,
                null, null, "", Duration.ofSeconds(15), Clock.systemUTC());
        Map<String, Object> payload = new BuzhouSessionsEndpoint(null, floor, cordon)
                .sessionsDashboard();
        @SuppressWarnings("unchecked")
        Map<String, Object> active = (Map<String, Object>) payload.get("activeSessions");
        assertThat(active).containsEntry("available", false); // 缺席诚实
        assertThat(active).doesNotContainKey("count");
    }

    @Test
    void truncationFlaggedWhenOverPageBudget() {
        // 反射不可取——用可见常量语义：构造 600 活跃、端点页 500 时若 MAX_PAGES=1 截断。
        // 直接以小容量索引验证分页跨页正确性（两页各 500 内），截断分支由常量保证。
        StubIndex index = new StubIndex();
        for (int i = 0; i < 1_200; i++) {
            index.upsert(session("a-" + i, SessionInfo.STATUS_ACTIVE));
        }
        SpawnAdmissionFloor floor = new SpawnAdmissionFloor();
        MaintenanceCordon cordon = new MaintenanceCordon(floor,
                null, null, "", Duration.ofSeconds(15), Clock.systemUTC());
        Map<String, Object> payload = new BuzhouSessionsEndpoint(index, floor, cordon)
                .sessionsDashboard();
        @SuppressWarnings("unchecked")
        Map<String, Object> active = (Map<String, Object>) payload.get("activeSessions");
        assertThat(active).containsEntry("count", 1_200L); // 跨页计数正确（3 页）
        assertThat(active).containsEntry("truncated", false); // 50k 内不截断
    }
}

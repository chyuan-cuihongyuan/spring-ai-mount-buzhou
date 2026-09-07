package io.github.chyuan_cuihongyuan.buzhou.core.health;

import io.github.chyuan_cuihongyuan.buzhou.core.backpressure.MaintenanceCordon;
import io.github.chyuan_cuihongyuan.buzhou.core.backpressure.SpawnAdmissionFloor;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 会话面板端点 {@code /actuator/buzhou-sessions}（spec 346 / T683，
 * 面板三部曲之三——343 配置/345 告警/本端点会话）：活跃会话数（索引
 * status=ACTIVE 分页计数——318 PDB 同事实源）+ spawn 准入地板（effective
 * + 各源——342 多源：谁抬着一目了然）+ 维护 cordon 视图。
 *
 * <p>无索引 bean → activeSessions 段 `available: false` 诚实缺席；计数
 * 50k 封顶（页 500 × 至多 100 页）超限 `truncated: true` 诚实降级。只读。
 */
@Endpoint(id = "buzhou-sessions")
public final class BuzhouSessionsEndpoint {

    /** 页大小对齐 SessionIndexQuery 契约上限（≤200——SPI 静默夹紧，超出会提前断页）。 */
    static final int PAGE_SIZE = 200;
    static final int MAX_PAGES = 250; // 200×250 = 50k 计数封顶

    private final SessionIndexStore index;              // 可空——无索引部署
    private final SpawnAdmissionFloor floor;            // 恒在（342）
    private final MaintenanceCordon cordon;             // 恒在（342）

    public BuzhouSessionsEndpoint(SessionIndexStore index, SpawnAdmissionFloor floor,
            MaintenanceCordon cordon) {
        this.index = index;
        this.floor = floor;
        this.cordon = cordon;
    }

    @ReadOperation
    public Map<String, Object> sessionsDashboard() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("activeSessions", activeSessionsSection());
        Map<String, Object> floorSection = new LinkedHashMap<>();
        floorSection.put("effective", floor == null ? null : floor.get().name());
        floorSection.put("sources", floor == null ? Map.of() : sourceNames(floor));
        payload.put("spawnFloor", floorSection);
        payload.put("maintenance", cordon == null ? Map.of() : cordon.view());
        return payload;
    }

    private Map<String, Object> activeSessionsSection() {
        Map<String, Object> section = new LinkedHashMap<>();
        if (index == null) {
            section.put("available", false);
            return section;
        }
        long count = 0;
        boolean truncated = false;
        for (int page = 0; page < MAX_PAGES; page++) {
            List<SessionInfo> batch = index.list(new SessionIndexQuery(
                    null, null, SessionInfo.STATUS_ACTIVE, null, null,
                    page * PAGE_SIZE, PAGE_SIZE));
            count += batch.size();
            if (batch.size() < PAGE_SIZE) {
                break;
            }
            if (page == MAX_PAGES - 1) {
                truncated = true; // 50k 封顶——诚实降级
            }
        }
        section.put("available", true);
        section.put("count", count);
        section.put("truncated", truncated);
        return section;
    }

    private static Map<String, String> sourceNames(SpawnAdmissionFloor floor) {
        Map<String, String> sources = new LinkedHashMap<>();
        floor.view().forEach((source, priority) -> sources.put(source, priority.name()));
        return sources;
    }
}

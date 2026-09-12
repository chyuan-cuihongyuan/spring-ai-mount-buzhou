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
    /** 可空——无 state 读面部署（forkedActive 段诚实缺席）。 */
    private final io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore stateStore;

    /** spec 627：fork 谱系 state 键（与 DefaultAgentRuntime 写入口径一致——测试双向钉住）。 */
    static final String FORK_SOURCE_STATE_KEY = "buzhou.fork.source";

    /** affinity 展示桶数（spec 415：buzhou.sessions.affinity-buckets，默认 16）。 */
    private final int affinityBuckets;

    public BuzhouSessionsEndpoint(SessionIndexStore index, SpawnAdmissionFloor floor,
            MaintenanceCordon cordon) {
        this(index, floor, cordon, 16);
    }

    public BuzhouSessionsEndpoint(SessionIndexStore index, SpawnAdmissionFloor floor,
            MaintenanceCordon cordon, int affinityBuckets) {
        this(index, floor, cordon, affinityBuckets, null);
    }

    /** spec 627 / T904：带 state 读面构造（forkedActive 段的谱系计数）。 */
    public BuzhouSessionsEndpoint(SessionIndexStore index, SpawnAdmissionFloor floor,
            MaintenanceCordon cordon, int affinityBuckets,
            io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore stateStore) {
        this.index = index;
        this.floor = floor;
        this.cordon = cordon;
        this.affinityBuckets = Math.max(1, affinityBuckets);
        this.stateStore = stateStore;
    }

    @ReadOperation
    public Map<String, Object> sessionsDashboard() {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("activeSessions", activeSessionsSection());
        payload.put("forkedActive", forkedActiveSection());
        Map<String, Object> floorSection = new LinkedHashMap<>();
        floorSection.put("effective", floor == null ? null : floor.get().name());
        floorSection.put("sources", floor == null ? Map.of() : sourceNames(floor));
        payload.put("spawnFloor", floorSection);
        payload.put("maintenance", cordon == null ? Map.of() : cordon.view());
        payload.put("affinity", affinitySection());
        return payload;
    }

    /**
     * spec 415 / T722：黏性路由提示——活跃会话（首页封顶 50 行）的亲和键/桶位。
     * 纯函数跨实例一致；buckets 仅展示位零行为变化。
     */
    private Map<String, Object> affinitySection() {
        Map<String, Object> section = new LinkedHashMap<>();
        section.put("buckets", affinityBuckets);
        section.put("recipe", "LB 按 sha256(appId|sessionId) 前 8 hex 哈希（nginx: hash $arg_affinity consistent）");
        if (index == null) {
            section.put("available", false);
            section.put("rows", List.of());
            return section;
        }
        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        List<SessionInfo> batch = index.list(new SessionIndexQuery(
                null, null, SessionInfo.STATUS_ACTIVE, null, null, 0, 50));
        for (SessionInfo info : batch) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("sessionId", info.sessionId());
            row.put("appId", info.appId());
            row.put("affinityKey",
                    io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAffinity.key(
                            info.appId(), info.sessionId()));
            row.put("affinityBucket",
                    io.github.chyuan_cuihongyuan.buzhou.core.session.SessionAffinity.bucket(
                            info.appId(), info.sessionId(), affinityBuckets));
            rows.add(row);
        }
        section.put("available", true);
        section.put("rows", rows);
        return section;
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

    /**
     * spec 627 / T904：fork 谱系活跃段——活跃会话中带 {@code buzhou.fork.source} 的计数
     * （重试/探索分支流量信号）。state 读面缺席 = 段 {@code available:false} 诚实缺席；
     * 每会话一次 state GET（ops 按需面板，≤50k 封顶同计数循环）。
     */
    private Map<String, Object> forkedActiveSection() {
        Map<String, Object> section = new LinkedHashMap<>();
        if (index == null || stateStore == null) {
            section.put("available", false);
            return section;
        }
        long forked = 0;
        for (int page = 0; page < MAX_PAGES; page++) {
            List<SessionInfo> batch = index.list(new SessionIndexQuery(
                    null, null, SessionInfo.STATUS_ACTIVE, null, null,
                    page * PAGE_SIZE, PAGE_SIZE));
            for (SessionInfo info : batch) {
                if (stateStore.get(info.sessionId(), FORK_SOURCE_STATE_KEY).isPresent()) {
                    forked++;
                }
            }
            if (batch.size() < PAGE_SIZE) {
                break;
            }
        }
        section.put("available", true);
        section.put("count", forked);
        return section;
    }

    private static Map<String, String> sourceNames(SpawnAdmissionFloor floor) {
        Map<String, String> sources = new LinkedHashMap<>();
        floor.view().forEach((source, priority) -> sources.put(source, priority.name()));
        return sources;
    }
}

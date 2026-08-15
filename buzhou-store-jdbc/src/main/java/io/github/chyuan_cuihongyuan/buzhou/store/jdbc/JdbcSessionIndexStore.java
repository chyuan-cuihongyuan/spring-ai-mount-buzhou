package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 会话索引 JDBC 实现（spec 30 / T109 / impl-84）：表 buzhou_session_index（V3 迁移）。
 * upsert = UPDATE-then-INSERT（跨方言可移植；最终一致口径下罕见并发竞态由后者覆盖收敛）。
 * tags 以 JSON 列存储。查询按 last_active_at 倒序 + 动态过滤 + LIMIT/OFFSET。
 */
public class JdbcSessionIndexStore implements SessionIndexStore {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final System.Logger LOGGER = System.getLogger(JdbcSessionIndexStore.class.getName());

    private static final RowMapper<SessionInfo> MAPPER_ROW = (rs, n) -> new SessionInfo(
            rs.getString("session_id"),
            rs.getString("app_id"),
            rs.getString("agent_name"),
            rs.getString("status"),
            rs.getLong("created_at_ms"),
            rs.getLong("last_active_at_ms"),
            rs.getInt("turn_count"),
            parseTags(rs.getString("tags")));

    private final JdbcTemplate jdbc;

    public JdbcSessionIndexStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void upsert(SessionInfo info) {
        int updated = jdbc.update("""
                UPDATE buzhou_session_index
                SET app_id = ?, agent_name = ?, status = ?, created_at_ms = ?,
                    last_active_at_ms = ?, turn_count = ?, tags = ?
                WHERE session_id = ?
                """, info.appId(), info.agentName(), info.status(), info.createdAtEpochMs(),
                info.lastActiveAtEpochMs(), info.turnCount(), writeTags(info.tags()), info.sessionId());
        if (updated == 0) {
            try {
                jdbc.update("""
                        INSERT INTO buzhou_session_index
                            (session_id, app_id, agent_name, status, created_at_ms, last_active_at_ms, turn_count, tags)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """, info.sessionId(), info.appId(), info.agentName(), info.status(),
                        info.createdAtEpochMs(), info.lastActiveAtEpochMs(), info.turnCount(),
                        writeTags(info.tags()));
            } catch (org.springframework.dao.DuplicateKeyException lostRace) {
                // 并发 upsert 竞态：对家已插入——重放一次 UPDATE 收敛（最终一致）
                jdbc.update("""
                        UPDATE buzhou_session_index
                        SET app_id = ?, agent_name = ?, status = ?, created_at_ms = ?,
                            last_active_at_ms = ?, turn_count = ?, tags = ?
                        WHERE session_id = ?
                        """, info.appId(), info.agentName(), info.status(), info.createdAtEpochMs(),
                        info.lastActiveAtEpochMs(), info.turnCount(), writeTags(info.tags()),
                        info.sessionId());
            }
        }
    }

    @Override
    public Optional<SessionInfo> get(String sessionId) {
        List<SessionInfo> rows = jdbc.query(
                "SELECT * FROM buzhou_session_index WHERE session_id = ?", MAPPER_ROW, sessionId);
        return rows.stream().findFirst();
    }

    @Override
    public List<SessionInfo> list(SessionIndexQuery query) {
        StringBuilder where = new StringBuilder(" WHERE 1=1");
        Map<String, Object> args = new LinkedHashMap<>();
        if (query.appId() != null) {
            where.append(" AND app_id = ?");
            args.put("appId", query.appId());
        }
        if (query.agentName() != null) {
            where.append(" AND agent_name = ?");
            args.put("agentName", query.agentName());
        }
        if (query.status() != null) {
            where.append(" AND status = ?");
            args.put("status", query.status());
        } else {
            where.append(" AND status <> 'DELETED'"); // 默认排除审计行（spec 33 §B）
        }
        if (query.tagKey() != null) {
            // tags 为 JSON 列：标签匹配走 JSON 文本 LIKE（索引量级下可接受；精确等值解析在内存侧收口）
            where.append(" AND tags LIKE ?");
            args.put("tagLike", "%\"" + query.tagKey() + "\":\"" + query.tagValue() + "\"%");
        }
        String sql = "SELECT * FROM buzhou_session_index" + where
                + " ORDER BY last_active_at_ms DESC, session_id LIMIT " + query.limit()
                + " OFFSET " + query.offset();
        List<SessionInfo> rows = jdbc.query(sql, MAPPER_ROW, args.values().toArray());
        // tag LIKE 命中含转义邻键的极小概率误报（如 k 与 k2 前缀重叠）：内存侧精确复核
        if (query.tagKey() != null) {
            return rows.stream()
                    .filter(info -> query.tagValue().equals(info.tags().get(query.tagKey())))
                    .toList();
        }
        return rows;
    }

    @Override
    public void delete(String sessionId) {
        jdbc.update("DELETE FROM buzhou_session_index WHERE session_id = ?", sessionId);
    }

    private static String writeTags(Map<String, String> tags) {
        try {
            return MAPPER.writeValueAsString(tags == null ? Map.of() : tags);
        } catch (Exception e) {
            LOGGER.log(System.Logger.Level.WARNING, "会话索引 tags 序列化失败（按空 tags 落库）：" + e.getMessage());
            return "{}";
        }
    }

    private static Map<String, String> parseTags(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return MAPPER.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {
            });
        } catch (Exception e) {
            return Map.of();
        }
    }
}

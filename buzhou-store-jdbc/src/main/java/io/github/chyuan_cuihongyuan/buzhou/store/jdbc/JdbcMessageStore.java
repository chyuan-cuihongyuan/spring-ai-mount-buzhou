package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.error.BuzhouDataCorruptionException;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 消息存储 JDBC 实现（spec 13 §stores-7 / ticket 32 脏数据隔离）。
 *
 * <p>load/findById 两阶段映射：先把 JSON 列原样取回（raw 行），再逐条解析——
 * 单条坏 JSON / 坏枚举只跳过该条（WARN + {@link BuzhouDataCorruptionException} 记录 +
 * {@link #corruptionCount()} 计数），绝不炸整个会话加载（29 号片异常类型的落地消费点）。
 */
public class JdbcMessageStore implements MessageStore {

    private static final Logger LOG = LoggerFactory.getLogger(JdbcMessageStore.class);

    /** raw 行：JSON 列（tool_calls/metadata）与 role 先原样保留，逐条解析时隔离坏行。 */
    private record RawMessageRow(
            String id,
            String sessionId,
            int turnSeq,
            int seqInTurn,
            String role,
            String content,
            String toolCallsJson,
            String toolCallId,
            String reasoningContent,
            String reasoningSignature,
            String metadataJson,
            java.time.Instant createdAt) {
    }

    private static final RowMapper<RawMessageRow> RAW_MAPPER = (rs, n) -> new RawMessageRow(
            rs.getString("id"),
            rs.getString("session_id"),
            rs.getInt("turn_seq"),
            rs.getInt("seq_in_turn"),
            rs.getString("role"),
            rs.getString("content"),
            rs.getString("tool_calls"),
            rs.getString("tool_call_id"),
            rs.getString("reasoning_content"),
            rs.getString("reasoning_signature"),
            rs.getString("metadata"),
            rs.getTimestamp("created_at").toInstant());

    private final JdbcTemplate jdbc;

    /** 脏数据计数（跳过不静默：可断言 / 可采集）。 */
    private final AtomicLong corruptionCount = new AtomicLong();

    public JdbcMessageStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void append(String sessionId, List<BuzhouMessage> messages) {
        jdbc.batchUpdate("""
                        INSERT INTO buzhou_message
                        (id, session_id, turn_seq, seq_in_turn, role, content, tool_calls,
                         tool_call_id, reasoning_content, reasoning_signature, metadata, created_at)
                        VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                        """,
                messages, messages.size(), (ps, m) -> {
                    ps.setString(1, m.id());
                    ps.setString(2, sessionId);
                    ps.setInt(3, m.turnSeq());
                    ps.setInt(4, m.seqInTurn());
                    ps.setString(5, m.role().name());
                    ps.setString(6, m.content());
                    ps.setString(7, JdbcJson.write(m.toolCalls()));
                    ps.setString(8, m.toolCallId());
                    ps.setString(9, m.reasoningContent());
                    ps.setString(10, m.reasoningSignature());
                    ps.setString(11, JdbcJson.write(m.metadata()));
                    ps.setTimestamp(12, Timestamp.from(m.createdAt()));
                });
    }

    @Override
    public List<BuzhouMessage> load(String sessionId) {
        // 逐条解析隔离脏行：返回其余健康消息（会话加载绝不因单条坏数据整盘失败）
        List<BuzhouMessage> result = new java.util.ArrayList<>();
        for (RawMessageRow row : jdbc.query(
                "SELECT * FROM buzhou_message WHERE session_id = ? ORDER BY turn_seq, seq_in_turn",
                RAW_MAPPER, sessionId)) {
            parseRow(sessionId, row).ifPresent(result::add);
        }
        return result;
    }

    @Override
    public Optional<BuzhouMessage> findById(String messageId) {
        return jdbc.query("SELECT * FROM buzhou_message WHERE id = ?", RAW_MAPPER, messageId)
                .stream().findFirst()
                .flatMap(row -> parseRow(row.sessionId(), row));
    }

    /** 单条解析（坏 JSON / 坏枚举 → WARN + 损坏计数 + 跳过）。 */
    private Optional<BuzhouMessage> parseRow(String sessionId, RawMessageRow row) {
        try {
            return Optional.of(new BuzhouMessage(
                    row.id(),
                    row.sessionId(),
                    row.turnSeq(),
                    row.seqInTurn(),
                    Role.valueOf(row.role()),
                    row.content(),
                    JdbcJson.readList(row.toolCallsJson(), ToolCallRecord.class),
                    row.toolCallId(),
                    row.reasoningContent(),
                    row.reasoningSignature(),
                    JdbcJson.readMap(row.metadataJson()),
                    row.createdAt()));
        } catch (RuntimeException e) {
            BuzhouDataCorruptionException corruption = new BuzhouDataCorruptionException(
                    "消息记录损坏已跳过(sessionId=%s, messageId=%s, turnSeq=%d, seqInTurn=%d)"
                            .formatted(sessionId, row.id(), row.turnSeq(), row.seqInTurn()), e);
            // 记录 BuzhouDataCorruptionException（含根因栈）：跳过不静默
            LOG.warn(corruption.getMessage(), corruption);
            corruptionCount.incrementAndGet();
            return Optional.empty();
        }
    }

    /** 累计跳过的脏消息条数（丢弃不可静默——测试与运维可断言该计数）。 */
    public long corruptionCount() {
        return corruptionCount.get();
    }

    /** impl-35 / spec 13 §stores-6：单表批量删（幂等；单语句自原子）。 */
    @Override
    public void deleteSession(String sessionId) {
        jdbc.update("DELETE FROM buzhou_message WHERE session_id = ?", sessionId);
    }
}

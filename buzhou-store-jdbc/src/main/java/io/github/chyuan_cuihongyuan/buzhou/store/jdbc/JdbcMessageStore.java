package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.message.ToolCallRecord;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.MessageStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class JdbcMessageStore implements MessageStore {

    private final JdbcTemplate jdbc;

    private static final RowMapper<BuzhouMessage> MAPPER = (rs, n) -> new BuzhouMessage(
            rs.getString("id"),
            rs.getString("session_id"),
            rs.getInt("turn_seq"),
            rs.getInt("seq_in_turn"),
            Role.valueOf(rs.getString("role")),
            rs.getString("content"),
            JdbcJson.readList(rs.getString("tool_calls"), ToolCallRecord.class),
            rs.getString("tool_call_id"),
            rs.getString("reasoning_content"),
            rs.getString("reasoning_signature"),
            JdbcJson.readMap(rs.getString("metadata")),
            rs.getTimestamp("created_at").toInstant());

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
        return jdbc.query(
                "SELECT * FROM buzhou_message WHERE session_id = ? ORDER BY turn_seq, seq_in_turn",
                MAPPER, sessionId);
    }

    @Override
    public Optional<BuzhouMessage> findById(String messageId) {
        return jdbc.query("SELECT * FROM buzhou_message WHERE id = ?", MAPPER, messageId)
                .stream().findFirst();
    }
}

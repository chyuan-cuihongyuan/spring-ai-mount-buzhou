package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 13 §stores-7 / ticket 32：脏数据隔离契约——一条坏 JSON → load 成功返回其余消息，
 * 且不静默（{@code corruptionCount()} 可断言）。core 契约基类（test-jar）不可改，
 * 故以模块内契约测试补齐同一断言语义。
 */
class H2DirtyDataIsolationTest {

    private JdbcBuzhouRecoveryStores stores;
    private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:dirty-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        stores = JdbcBuzhouStores.createWithRecovery(dataSource, Dialect.H2);
        jdbc = new JdbcTemplate(dataSource);
    }

    @Test
    void shouldLoadRemainingMessages_whenOneRowHasCorruptToolCallsJson() {
        String sessionId = "dirty-json-" + UUID.randomUUID();
        BuzhouMessage good1 = msg(sessionId, 1, 0);
        BuzhouMessage good2 = msg(sessionId, 3, 0);
        stores.messageStore().append(sessionId, List.of(good1, good2));
        insertRawMessage(sessionId, "dirty-json-" + UUID.randomUUID(), 2, 0, "{not-valid-json");

        List<BuzhouMessage> loaded = stores.messageStore().load(sessionId);

        assertThat(loaded).extracting(BuzhouMessage::id).containsExactly(good1.id(), good2.id());
        JdbcMessageStore messageStore = (JdbcMessageStore) stores.messageStore();
        assertThat(messageStore.corruptionCount()).isEqualTo(1L); // 跳过不静默
    }

    @Test
    void shouldLoadRemainingMessages_whenOneRowHasUnknownRole() {
        String sessionId = "dirty-role-" + UUID.randomUUID();
        BuzhouMessage good = msg(sessionId, 1, 0);
        stores.messageStore().append(sessionId, List.of(good));
        insertRawMessage(sessionId, "dirty-role-" + UUID.randomUUID(), 2, 0, null, "NOT_A_ROLE");

        assertThat(stores.messageStore().load(sessionId))
                .extracting(BuzhouMessage::id).containsExactly(good.id());
        assertThat(((JdbcMessageStore) stores.messageStore()).corruptionCount()).isEqualTo(1L);
    }

    @Test
    void shouldReturnEmpty_whenFindByIdHitsCorruptRow() {
        String sessionId = "dirty-find-" + UUID.randomUUID();
        String corruptId = "dirty-find-id-" + UUID.randomUUID();
        insertRawMessage(sessionId, corruptId, 1, 0, "{bad");

        assertThat(stores.messageStore().findById(corruptId)).isEmpty();
        assertThat(((JdbcMessageStore) stores.messageStore()).corruptionCount()).isEqualTo(1L);
    }

    private BuzhouMessage msg(String sessionId, int turnSeq, int seqInTurn) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turnSeq, seqInTurn,
                Role.USER, "content-" + turnSeq, List.of(), null, null, null, Map.of(), Instant.now());
    }

    /** 绕过 store 直插坏行（模拟外部写入 / 序列化版本不兼容遗留数据）。 */
    private void insertRawMessage(String sessionId, String id, int turnSeq, int seqInTurn,
                                  String corruptToolCalls) {
        insertRawMessage(sessionId, id, turnSeq, seqInTurn, corruptToolCalls, "USER");
    }

    private void insertRawMessage(String sessionId, String id, int turnSeq, int seqInTurn,
                                  String toolCalls, String role) {
        jdbc.update("""
                        INSERT INTO buzhou_message
                        (id, session_id, turn_seq, seq_in_turn, role, content, tool_calls,
                         tool_call_id, reasoning_content, reasoning_signature, metadata, created_at)
                        VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                        """,
                id, sessionId, turnSeq, seqInTurn, role, "c", toolCalls, null, null, null, null,
                Timestamp.from(Instant.now()));
    }
}

package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 会话索引 keyset 游标分页测试（spec 631 / T912–T913 / impl 484，PostgreSQL keyset
 * pagination 惯例）：翻页稳定（行活跃度变化不跳行不重行）、规范序平局、游标编解码
 * 校验、七参兼容。
 */
class SessionIndexKeysetTest {

    private static SessionInfo info(String id, long lastActiveAtMs) {
        return new SessionInfo(id, "app", "agent", SessionInfo.STATUS_ACTIVE,
                lastActiveAtMs, lastActiveAtMs, 1, Map.of());
    }

    /** 翻页稳定性：页间行活跃度变化——keyset 页边界不动（offset 会跳/重）。 */
    @Test
    void keysetPagesStableAcrossActivityChanges() {
        InMemorySessionIndexStore index = new InMemorySessionIndexStore();
        for (int i = 1; i <= 5; i++) {
            index.upsert(info("s" + i, 1000L + i));
        }

        List<SessionInfo> page1 = index.list(new SessionIndexQuery(
                null, null, null, null, null, 0, 2, null));
        assertThat(page1).extracting(SessionInfo::sessionId).containsExactly("s5", "s4");

        // 页间：s3 活跃度飙升到最高（offset 分页会把它「跳过」或重页）
        index.upsert(info("s3", 999_999L));

        List<SessionInfo> page2 = index.list(new SessionIndexQuery(
                null, null, null, null, null, 0, 2,
                SessionIndexQuery.encodeCursor(page1.get(page1.size() - 1))));
        // 游标锚定 (1003, s4)：s3 新活跃度 999999 在锚点之前（序更高）→ 不入本页
        assertThat(page2).extracting(SessionInfo::sessionId).containsExactly("s2", "s1");
    }

    /** 规范序平局：同活跃度按 sessionId 字典序升——游标过滤边界正确。 */
    @Test
    void tiesBreakBySessionIdAscending() {
        InMemorySessionIndexStore index = new InMemorySessionIndexStore();
        index.upsert(info("b", 500L));
        index.upsert(info("a", 500L));
        index.upsert(info("c", 500L));

        List<SessionInfo> page = index.list(new SessionIndexQuery(
                null, null, null, null, null, 0, 2, null));
        assertThat(page).extracting(SessionInfo::sessionId).containsExactly("a", "b");

        List<SessionInfo> next = index.list(new SessionIndexQuery(
                null, null, null, null, null, 0, 2,
                SessionIndexQuery.encodeCursor(page.get(1))));
        assertThat(next).extracting(SessionInfo::sessionId).containsExactly("c");
    }

    /** 游标编解码往返 + 非法格式 fail-fast + 七参兼容构造。 */
    @Test
    void cursorCodecAndValidation() {
        String cursor = SessionIndexQuery.encodeCursor(info("s9", 12345L));
        SessionIndexQuery.Cursor decoded = SessionIndexQuery.decodeCursor(cursor);
        assertThat(decoded.lastActiveAtEpochMs()).isEqualTo(12345L);
        assertThat(decoded.sessionId()).isEqualTo("s9");
        assertThat(SessionIndexQuery.encodeCursor(null)).isNull();

        assertThatThrownBy(() -> new SessionIndexQuery(
                null, null, null, null, null, 0, 20, "not-a-cursor"))
                .isInstanceOf(IllegalArgumentException.class);

        // 七参兼容（无游标）
        assertThat(new SessionIndexQuery(null, null, null, null, null, 0, 20).cursor()).isNull();
    }
}

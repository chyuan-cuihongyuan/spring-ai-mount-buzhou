package io.github.chyuan_cuihongyuan.buzhou.store.redis;

/**
 * Redis key 布局（spec 03 Redis 推演 + spec 08 key-prefix）。
 *
 * <p>前缀默认 {@code buzhou:}，可由装配侧覆盖（spec 08 {@code buzhou.store.redis.key-prefix}）。
 * key 命名按会话分桶——{@code deleteSession}（impl-35 / spec 13 §stores-6）按会话索引
 * （LIST/ZSET/SET）枚举成员成套清理；注入快照无索引（key 带 TTL），按
 * {@link #snapshotScanPattern(String)} SCAN 补删。
 *
 * <p>TTL 策略（spec 08 §451）：消息/摘要/状态/观测 span/event 不设 TTL（依赖 Redis AOF/RDB 持久化）；
 * 租约 key 与注入快照 key 设 TTL（租约 ttl / snapshot.ttl）。
 */
final class RedisKeys {

    private final String pfx;

    RedisKeys(String prefix) {
        this.pfx = (prefix == null || prefix.isBlank()) ? "buzhou:" : prefix;
    }

    // ---- message ----
    /** 会话消息列表（LIST of JSON，按 turnSeq,seqInTurn 排序在 load 时做）。 */
    String messageList(String sessionId) {
        return pfx + "msg:" + sessionId;
    }

    /** 单条消息按 id 索引（findById）。 */
    String messageById(String messageId) {
        return pfx + "msgid:" + messageId;
    }

    // ---- summary ----
    /** 会话摘要版本计数器（INCR 单调）。 */
    String summarySeq(String sessionId) {
        return pfx + "sum:" + sessionId + ":seq";
    }

    /** 某版本摘要正文（STRING JSON）。 */
    String summaryVersion(String sessionId, long version) {
        return pfx + "sum:" + sessionId + ":v:" + version;
    }

    /** 会话摘要版本索引（ZSET，score=version，member=version 字符串）。 */
    String summaryVersions(String sessionId) {
        return pfx + "sum:" + sessionId + ":versions";
    }

    // ---- session state ----
    /**
     * 单 state 条目（HASH：value/producer/createdTurn/ttlTurns/updatedAt）。
     * per-key hash 而非整袋 hash：让 {@code deleteIfValueMatches} 的 Lua 能直接 HGET value 比价，
     * 无需在 Lua 里解析 JSON。
     */
    String stateEntry(String sessionId, String key) {
        return pfx + "state:" + sessionId + ":" + key;
    }

    /** 会话 state 键集合（SET，成员为 state key）——getAll 遍历用。 */
    String stateKeys(String sessionId) {
        return pfx + "statekeys:" + sessionId;
    }

    // ---- lease ----
    /** 会话租约（HASH：owner/fencingToken/acquiredAt/expiresAt；key 带 TTL=租约 ttl）。 */
    String lease(String sessionId) {
        return pfx + "lease:" + sessionId;
    }

    /** 会话 fencing 单调计数器（INCR，无 TTL——保证 fencing token 不复用）。 */
    String leaseFencingSeq(String sessionId) {
        return pfx + "lease:" + sessionId + ":seq";
    }

    // ---- observability ----
    /** 单 span（HASH，字段见 RedisObservabilityStore；HSET 实现 upsert：RUNNING→终态覆盖同 spanId）。 */
    String span(String sessionId, String spanId) {
        return pfx + "obs:" + sessionId + ":span:" + spanId;
    }

    /** 会话内 span 索引（ZSET，score=startedAt 毫秒，member=spanId）。 */
    String spansOfSession(String sessionId) {
        return pfx + "obs:" + sessionId + ":spans";
    }

    /** 全局会话活跃索引（ZSET，score=lastActivity 毫秒，member=sessionId）——listSessionSummaries 数据源。 */
    String sessionsIndex() {
        return pfx + "obs:sessions";
    }

    /** 单 event 正文（STRING JSON，全局按 eventId 索引）。 */
    String event(String eventId) {
        return pfx + "obs:event:" + eventId;
    }

    /** 会话内 event 索引（ZSET，score=occurredAt 毫秒，member=eventId）。 */
    String eventsOfSession(String sessionId) {
        return pfx + "obs:" + sessionId + ":events";
    }

    /** 单 span 的 event 索引（ZSET，score=occurredAt 毫秒，member=eventId）。 */
    String eventsOfSpan(String spanId) {
        return pfx + "obs:spev:" + spanId;
    }

    /** 注入快照（STRING JSON，key 带 TTL=snapshot.ttl）。 */
    String snapshot(String sessionId, int turnSeq) {
        return pfx + "obs:" + sessionId + ":snap:" + turnSeq;
    }

    /**
     * 注入快照 SCAN 模式（impl-35：快照 key 无索引，deleteSession 按会话桶模式补删；
     * sessionId 中的 glob 元字符转义，防跨会话误删）。
     */
    String snapshotScanPattern(String sessionId) {
        return pfx + "obs:" + escapeGlob(sessionId) + ":snap:*";
    }

    private static String escapeGlob(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '*' || ch == '?' || ch == '[' || ch == ']' || ch == '\\') {
                sb.append('\\');
            }
            sb.append(ch);
        }
        return sb.toString();
    }
}

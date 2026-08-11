package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.lettuce.core.ScriptOutputType;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Redis {@link SessionStateStore}：每条 state 存独立 HASH（字段含原始 value），
 * 会话级 SET 索引其 key 集合。
 *
 * <p>{@link #deleteIfValueMatches} 经 Lua 原子「读 value 比价 → DEL + SREM」，
 * 满足 HITL 一次性授权的 CAS 语义（spec 07）；per-key hash 设计使 Lua 无需解析 JSON。
 */
public class RedisSessionStateStore implements SessionStateStore {

    /** Lua CAS：HGET value 匹配则 DEL + SREM，返回 1；否则 0。KEYS=[entryKey, setKey] ARGV=[expected, stateKey]。 */
    private static final String CAS_SCRIPT = """
            if redis.call('HGET', KEYS[1], 'value') == ARGV[1] then
              redis.call('DEL', KEYS[1])
              redis.call('SREM', KEYS[2], ARGV[2])
              return 1
            else
              return 0
            end""";

    private final RedisSync sync;
    private final RedisKeys keys;

    public RedisSessionStateStore(RedisSync sync, RedisKeys keys) {
        this.sync = sync;
        this.keys = keys;
    }

    @Override
    public void put(String sessionId, StateEntry entry) {
        var c = sync.commands();
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("key", entry.key());
        fields.put("value", entry.value());
        fields.put("producer", entry.producer());
        fields.put("createdTurn", Integer.toString(entry.createdTurn()));
        fields.put("ttlTurns", entry.ttlTurns() == null ? "" : Integer.toString(entry.ttlTurns()));
        fields.put("updatedAt", entry.updatedAt().toString());
        c.hset(keys.stateEntry(sessionId, entry.key()), fields);
        c.sadd(keys.stateKeys(sessionId), entry.key());
    }

    @Override
    public Optional<StateEntry> get(String sessionId, String key) {
        Map<String, String> fields = sync.commands().hgetall(keys.stateEntry(sessionId, key));
        return Optional.ofNullable(fromHash(fields));
    }

    @Override
    public Map<String, StateEntry> getAll(String sessionId) {
        var c = sync.commands();
        Set<String> keySet = c.smembers(keys.stateKeys(sessionId));
        Map<String, StateEntry> out = new LinkedHashMap<>();
        if (keySet != null) {
            for (String k : keySet) {
                Map<String, String> fields = c.hgetall(keys.stateEntry(sessionId, k));
                StateEntry e = fromHash(fields);
                if (e != null) {
                    out.put(k, e);
                }
            }
        }
        return out;
    }

    @Override
    public void delete(String sessionId, String key) {
        var c = sync.commands();
        c.del(keys.stateEntry(sessionId, key));
        c.srem(keys.stateKeys(sessionId), key);
    }

    @Override
    public boolean deleteIfValueMatches(String sessionId, String key, String expectedValue) {
        Long hit = sync.commands().eval(CAS_SCRIPT, ScriptOutputType.INTEGER,
                new String[]{keys.stateEntry(sessionId, key), keys.stateKeys(sessionId)},
                expectedValue, key);
        return hit != null && hit == 1L;
    }

    /** Lua 原子 put-if-absent（幂等去重 reserve）：键不存在才 HSET + SADD，返回 1；否则 0。KEYS=[entryKey, setKey] ARGV=[stateKey, 各字段...]。 */
    private static final String PUT_IF_ABSENT_SCRIPT = """
            if redis.call('EXISTS', KEYS[1]) == 1 then
              return 0
            end
            redis.call('HSET', KEYS[1], 'key', ARGV[1], 'value', ARGV[2], 'producer', ARGV[3],
                       'createdTurn', ARGV[4], 'ttlTurns', ARGV[5], 'updatedAt', ARGV[6])
            redis.call('SADD', KEYS[2], ARGV[1])
            return 1""";

    @Override
    public boolean putIfAbsent(String sessionId, StateEntry entry) {
        Long inserted = sync.commands().eval(PUT_IF_ABSENT_SCRIPT, ScriptOutputType.INTEGER,
                new String[]{keys.stateEntry(sessionId, entry.key()), keys.stateKeys(sessionId)},
                entry.key(), entry.value(), entry.producer(),
                Integer.toString(entry.createdTurn()),
                entry.ttlTurns() == null ? "" : Integer.toString(entry.ttlTurns()),
                entry.updatedAt().toString());
        return inserted != null && inserted == 1L;
    }

    private static StateEntry fromHash(Map<String, String> fields) {
        if (fields == null || fields.isEmpty()) {
            return null;
        }
        String ttl = fields.get("ttlTurns");
        return new StateEntry(
                fields.get("key"),
                fields.get("value"),
                fields.get("producer"),
                Integer.parseInt(fields.get("createdTurn")),
                (ttl == null || ttl.isEmpty()) ? null : Integer.parseInt(ttl),
                Instant.parse(fields.get("updatedAt")));
    }
}

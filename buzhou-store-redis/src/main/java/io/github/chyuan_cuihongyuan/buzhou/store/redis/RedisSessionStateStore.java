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

    /** spec 33 §C / T114：键集合侧前缀过滤后按需 hgetall（免全量条目读）。 */
    @Override
    public Map<String, StateEntry> scanByPrefix(String sessionId, String prefix) {
        Map<String, StateEntry> result = new java.util.LinkedHashMap<>();
        var c = sync.commands();
        for (String k : c.smembers(keys.stateKeys(sessionId))) {
            if (k.startsWith(prefix)) {
                Map<String, String> fields = c.hgetall(keys.stateEntry(sessionId, k));
                StateEntry entry = fromHash(fields);
                if (entry != null) {
                    result.put(k, entry);
                }
            }
        }
        return result;
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

    /** impl-35 / spec 13 §stores-6：按会话键集删——SET 索引枚举删各 HASH，再删索引。幂等。 */
    @Override
    public void deleteSession(String sessionId) {
        var c = sync.commands();
        Set<String> keySet = c.smembers(keys.stateKeys(sessionId));
        if (keySet != null && !keySet.isEmpty()) {
            String[] entryKeys = keySet.stream()
                    .map(k -> keys.stateEntry(sessionId, k))
                    .toArray(String[]::new);
            c.del(entryKeys);
        }
        c.del(keys.stateKeys(sessionId));
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

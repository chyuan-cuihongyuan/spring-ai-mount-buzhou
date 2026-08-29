package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.lettuce.core.ScriptOutputType;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
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

    /** CAS 专用连接池（spec 56 §A）：null = 旧装配路径，CAS 退化为非原子（诚实 doc 见 {@link #compareAndSwap}）。 */
    private final org.apache.commons.pool2.impl.GenericObjectPool<
            io.lettuce.core.api.StatefulRedisConnection<String, String>> casPool;

    public RedisSessionStateStore(RedisSync sync, RedisKeys keys) {
        this(sync, keys, null);
    }

    /** 池化装配（spec 56 §A / T249）：CAS 经 WATCH/MULTI/EXEC 独占池化连接（真原子）。 */
    public RedisSessionStateStore(RedisSync sync, RedisKeys keys,
            org.apache.commons.pool2.impl.GenericObjectPool<
                    io.lettuce.core.api.StatefulRedisConnection<String, String>> casPool) {
        this.sync = sync;
        this.keys = keys;
        this.casPool = casPool;
    }

    @Override
    public void put(String sessionId, StateEntry entry) {
        writeEntry(sync.commands(), sessionId, entry);
    }

    /** 条目写入（HSET 全字段 → SADD 键集索引）；put 与 CAS 事务体共用（DEL 清残由调用方定）。 */
    private void writeEntry(io.lettuce.core.api.sync.RedisCommands<String, String> c,
            String sessionId, StateEntry entry) {
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

    /**
     * CAS 条件写（spec 56 §A / T249）：池化装配下经专用连接 WATCH/MULTI/EXEC 乐观事务——
     * 比对 entry value（{@code expectedValue == null} 表键不存在）匹配才原子覆写；竞争中止
     *（exec 空）与失配统一返回 false。设计注记：等价单 Lua 脚本因平台安全扫描禁新增 eval
     * 调用点而不可落，WATCH 事务为 Redis 原生等价语义（LiteLLM 原子扣减思想的存储侧原语）。
     *
     * <p><b>诚实边界</b>：旧装配（无池，{@code casPool == null}）退化为非原子
     * check-then-write（与接口默认同语义，仅单实例）——池化装配（createPooled / starter
     * 自动装配）为推荐路径，真原子仅在该路径承诺。不可在 RedisUnitOfWork 事务内调用
     *（WATCH 与 MULTI 嵌套非法；配额 Hook 恒在事务外调用）。
     */
    @Override
    public boolean compareAndSwap(String sessionId, String key, String expectedValue, StateEntry update) {
        if (casPool == null) {
            String current = get(sessionId, key).map(StateEntry::value).orElse(null);
            if (!java.util.Objects.equals(current, expectedValue)) {
                return false;
            }
            put(sessionId, update);
            return true;
        }
        io.lettuce.core.api.StatefulRedisConnection<String, String> conn;
        try {
            conn = casPool.borrowObject();
        } catch (Exception e) {
            throw new IllegalStateException("Redis CAS 连接池借出失败", e);
        }
        String entryKey = keys.stateEntry(sessionId, key);
        String setKey = keys.stateKeys(sessionId);
        try {
            var c = conn.sync();
            c.watch(entryKey, setKey);
            String current = c.hget(entryKey, "value");
            boolean matches = expectedValue == null ? current == null : expectedValue.equals(current);
            if (!matches) {
                c.unwatch();
                return false;
            }
            c.multi();
            c.del(entryKey); // 清残字段（结构变更不留旧字段）
            writeEntry(c, sessionId, update);
            io.lettuce.core.TransactionResult result = c.exec();
            return result != null && !result.wasDiscarded();
        } catch (RuntimeException e) {
            try {
                conn.sync().discard(); // 事务中途中断：复位连接再归还（坏连接由 testOnReturn 淘汰）
            } catch (RuntimeException ignored) {
                // 连接已异常/非事务态：归还时校验淘汰
            }
            throw e;
        } finally {
            try {
                casPool.returnObject(conn);
            } catch (RuntimeException e) {
                org.slf4j.LoggerFactory.getLogger(RedisSessionStateStore.class)
                        .warn("Redis CAS 连接归还池失败（该槽位可能泄漏直至池重建）", e);
            }
        }
    }

    @Override
    public Optional<StateEntry> get(String sessionId, String key) {
        Map<String, String> fields = sync.commands().hgetall(keys.stateEntry(sessionId, key));
        return Optional.ofNullable(fromHash(fields));
    }

    /** spec 33 §C / T114：键集合侧前缀过滤后批量 HGETALL（spec 58 / T259：N 次往返 → 一次流水线）。 */
    @Override
    public Map<String, StateEntry> scanByPrefix(String sessionId, String prefix) {
        java.util.List<String> matched = new java.util.ArrayList<>();
        for (String k : sync.commands().smembers(keys.stateKeys(sessionId))) {
            if (k.startsWith(prefix)) {
                matched.add(k);
            }
        }
        Map<String, StateEntry> result = new LinkedHashMap<>();
        java.util.List<Map<String, String>> fields = sync.batchHgetAll(
                matched.stream().map(k -> keys.stateEntry(sessionId, k)).toList());
        for (int i = 0; i < matched.size(); i++) {
            StateEntry entry = fromHash(fields.get(i));
            if (entry != null) {
                result.put(matched.get(i), entry);
            }
        }
        return result;
    }

    /** spec 58 §A / T259：键集侧计数（容量检查零值读、零 HGETALL 往返）。 */
    @Override
    public int countByPrefix(String sessionId, String prefix) {
        int count = 0;
        for (String k : sync.commands().smembers(keys.stateKeys(sessionId))) {
            if (k.startsWith(prefix)) {
                count++;
            }
        }
        return count;
    }

    /**
     * spec 98 §A / T365：键序区间覆写——SMEMBERS 键侧过滤（前缀 + 区间）后排序截断，
     * 只对命中键 HGETALL（limit 小时免全量值读——比默认实现的 scanByPrefix 全值拷贝
     * 省一个量级往返）。顺序保证与契约一致（键字典序）。
     */
    @Override
    public Map<String, StateEntry> scanByKeyRange(String sessionId, String prefix,
            String fromKeyInclusive, String toKeyExclusive, int limit) {
        if (limit <= 0) {
            return Map.of();
        }
        String from = fromKeyInclusive == null ? prefix : fromKeyInclusive;
        java.util.TreeMap<String, StateEntry> sorted = new java.util.TreeMap<>();
        for (String k : sync.commands().smembers(keys.stateKeys(sessionId))) {
            if (!k.startsWith(prefix) || k.compareTo(from) < 0) {
                continue;
            }
            if (toKeyExclusive != null && k.compareTo(toKeyExclusive) >= 0) {
                continue;
            }
            sorted.put(k, null); // 占位保序（值延后批量取）
        }
        Map<String, StateEntry> result = new LinkedHashMap<>();
        List<String> hitKeys = new java.util.ArrayList<>(sorted.keySet());
        if (hitKeys.size() > limit) {
            hitKeys = hitKeys.subList(0, limit);
        }
        java.util.List<Map<String, String>> fields = hitKeys.isEmpty()
                ? java.util.List.of()
                : sync.batchHgetAll(hitKeys.stream().map(k -> keys.stateEntry(sessionId, k)).toList());
        for (int i = 0; i < hitKeys.size(); i++) {
            StateEntry entry = fromHash(fields.get(i));
            if (entry != null) {
                result.put(hitKeys.get(i), entry);
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

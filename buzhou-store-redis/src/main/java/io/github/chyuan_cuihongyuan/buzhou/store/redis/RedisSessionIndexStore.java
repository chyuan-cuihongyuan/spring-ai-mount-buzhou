package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexQuery;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionIndexStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 会话索引 Redis 实现（spec 30 / T109 / impl-84）：ZSET（score=lastActiveAt 倒序面）+
 * STRING（sessionId → SessionInfo JSON）。查询 = ZREVRANGEBYSCORE 全量翻页后内存过滤
 * （索引量级 = 活跃会话数，翻页可接受；精确优化记 fog）。
 */
public class RedisSessionIndexStore implements SessionIndexStore, AutoCloseable {

    private final RedisSync sync;
    private final RedisKeys keys;
    private final io.lettuce.core.api.StatefulRedisConnection<String, String> ownConnection;

    RedisSessionIndexStore(RedisSync sync, RedisKeys keys,
            io.lettuce.core.api.StatefulRedisConnection<String, String> ownConnection) {
        this.sync = sync;
        this.keys = keys;
        this.ownConnection = ownConnection;
    }

    /** 装配工厂（自持连接——索引写频低，不占事务池；AutoCloseable 随 bean 生命周期关闭）。 */
    public static RedisSessionIndexStore create(io.lettuce.core.RedisClient client, String keyPrefix) {
        io.lettuce.core.api.StatefulRedisConnection<String, String> connection = client.connect();
        return new RedisSessionIndexStore(new RedisSync(connection), new RedisKeys(keyPrefix),
                connection);
    }

    @Override
    public void close() {
        ownConnection.close();
    }

    @Override
    public void upsert(SessionInfo info) {
        sync.commands().zadd(keys.sessionIndexZset(), info.lastActiveAtEpochMs(), info.sessionId());
        sync.commands().set(keys.sessionIndexInfo(info.sessionId()), RedisJson.write(info));
    }

    @Override
    public Optional<SessionInfo> get(String sessionId) {
        String json = sync.commands().get(keys.sessionIndexInfo(sessionId));
        return json == null ? Optional.empty() : Optional.of(RedisJson.read(json, SessionInfo.class));
    }

    @Override
    public List<SessionInfo> list(SessionIndexQuery query) {
        long total = sync.commands().zcard(keys.sessionIndexZset());
        List<SessionInfo> matched = new ArrayList<>();
        int page = Math.max(query.limit() * 4, 100);
        long cursor = 0;
        while (cursor < total && matched.size() < query.offset() + query.limit()) {
            List<String> ids = sync.commands().zrevrange(keys.sessionIndexZset(), cursor,
                    cursor + page - 1);
            if (ids.isEmpty()) {
                break;
            }
            for (String id : ids) {
                SessionInfo info = get(id).orElse(null);
                if (info != null && matches(info, query)) {
                    matched.add(info);
                }
            }
            cursor += page;
        }
        return matched.stream()
                .skip(query.offset())
                .limit(query.limit())
                .toList();
    }

    @Override
    public void delete(String sessionId) {
        sync.commands().zrem(keys.sessionIndexZset(), sessionId);
        sync.commands().del(keys.sessionIndexInfo(sessionId));
    }

    private static boolean matches(SessionInfo info, SessionIndexQuery q) {
        if (q.appId() != null && !q.appId().equals(info.appId())) {
            return false;
        }
        if (q.agentName() != null && !q.agentName().equals(info.agentName())) {
            return false;
        }
        if (q.status() == null ? SessionInfo.STATUS_DELETED.equals(info.status())
                : !q.status().equals(info.status())) {
            return false; // 默认排除 DELETED（审计行仅显式过滤可见——spec 33 §B）
        }
        return q.tagKey() == null || q.tagValue().equals(info.tags().get(q.tagKey()));
    }
}

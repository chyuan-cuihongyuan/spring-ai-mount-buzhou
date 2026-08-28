package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.error.QuotaExceededException;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.InMemoryStoreConfig;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.SessionStateStore;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话状态内存实现（事实台账 = noeviction 语义）。
 *
 * <p>impl-36 / spec 13 §growth-8：新会话写入超过 {@code maxSessions} 抛
 * {@link QuotaExceededException}（绝不静默丢）。
 */
public class InMemorySessionStateStore implements SessionStateStore {

    /** impl-36：准入串行化（检查与写入同临界区——原子拒绝）。 */
    private final Object admissionLock = new Object();

    private final ConcurrentHashMap<String, ConcurrentHashMap<String, StateEntry>> bySession =
            new ConcurrentHashMap<>();
    private final int maxSessions;

    public InMemorySessionStateStore() {
        this(InMemoryStoreConfig.defaults());
    }

    public InMemorySessionStateStore(InMemoryStoreConfig config) {
        InMemoryStoreConfig effective = config == null ? InMemoryStoreConfig.defaults() : config;
        this.maxSessions = effective.maxSessions();
    }

    @Override
    public void put(String sessionId, StateEntry entry) {
        synchronized (admissionLock) {
            if (!bySession.containsKey(sessionId) && bySession.size() >= maxSessions) {
                throw new QuotaExceededException(
                        "内存状态存储会话数已达上限 maxSessions=%d（sessionId=%s）"
                                .formatted(maxSessions, sessionId));
            }
            bySession.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>())
                    .put(entry.key(), entry);
        }
    }

    @Override
    public Optional<StateEntry> get(String sessionId, String key) {
        ConcurrentHashMap<String, StateEntry> session = bySession.get(sessionId);
        return session == null ? Optional.empty() : Optional.ofNullable(session.get(key));
    }

    @Override
    public Map<String, StateEntry> getAll(String sessionId) {
        ConcurrentHashMap<String, StateEntry> session = bySession.get(sessionId);
        return session == null ? Map.of() : Map.copyOf(session);
    }

    @Override
    public void delete(String sessionId, String key) {
        ConcurrentHashMap<String, StateEntry> session = bySession.get(sessionId);
        if (session != null) {
            session.remove(key);
        }
    }

    @Override
    public boolean deleteIfValueMatches(String sessionId, String key, String expectedValue) {
        ConcurrentHashMap<String, StateEntry> session = bySession.get(sessionId);
        if (session == null) {
            return false;
        }
        // CHM compute 对单 key 原子：value 匹配才置 null（删除）
        boolean[] removed = {false};
        session.computeIfPresent(key, (k, e) -> {
            if (java.util.Objects.equals(e.value(), expectedValue)) {
                removed[0] = true;
                return null;
            }
            return e;
        });
        return removed[0];
    }

    /** impl-35 / spec 13 §stores-6：移除该会话全部 state 条目（幂等）。 */
    @Override
    public void deleteSession(String sessionId) {
        bySession.remove(sessionId);
    }

    /** spec 58 §A / T259：键迭代计数（值零读）。 */
    @Override
    public int countByPrefix(String sessionId, String prefix) {
        ConcurrentHashMap<String, StateEntry> session = bySession.get(sessionId);
        if (session == null) {
            return 0;
        }
        int count = 0;
        for (String k : session.keySet()) {
            if (k.startsWith(prefix)) {
                count++;
            }
        }
        return count;
    }

    /** impl-36：在册会话数（测试与运维可观测）。 */
    int sessionCount() {
        return bySession.size();
    }

    /** spec 56 §A / T249：CAS 条件写——compute 对单 key 原子（含 absent 分支与准入上限）。 */
    @Override
    public boolean compareAndSwap(String sessionId, String key, String expectedValue, StateEntry update) {
        ConcurrentHashMap<String, StateEntry> session = bySession.get(sessionId);
        if (session == null) {
            // 新会话才进准入临界区（避免既有会话的 CAS 在全局锁上排队——热路径船队效应）
            synchronized (admissionLock) {
                if (!bySession.containsKey(sessionId) && bySession.size() >= maxSessions) {
                    throw new QuotaExceededException(
                            "内存状态存储会话数已达上限 maxSessions=%d（sessionId=%s）"
                                    .formatted(maxSessions, sessionId));
                }
                session = bySession.computeIfAbsent(sessionId, k -> new ConcurrentHashMap<>());
            }
        }
        boolean[] swapped = {false};
        session.compute(key, (k, cur) -> {
            String curValue = cur == null ? null : cur.value();
            if (java.util.Objects.equals(curValue, expectedValue)) {
                swapped[0] = true;
                return update;
            }
            return cur;
        });
        return swapped[0];
    }
}

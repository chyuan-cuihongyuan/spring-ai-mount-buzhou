package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.util.Map;
import java.util.Optional;

public interface SessionStateStore {

    void put(String sessionId, StateEntry entry);

    Optional<StateEntry> get(String sessionId, String key);

    Map<String, StateEntry> getAll(String sessionId);

    void delete(String sessionId, String key);

    /**
     * 条件删除（CAS）：仅当当前 value 与 expectedValue 相等时删除，返回是否删除成功。
     * HITL 一次性授权「放行即消费」的原子语义依赖本方法（spec 07 存储节定案）：
     * 多实例并发续跑消费同一授权时，只有一个实例删除成功获得放行。
     * JDBC 用带 value 条件的 DELETE（影响行数判定），Redis 用 Lua 比价后 DEL，内存用 CAS。
     */
    boolean deleteIfValueMatches(String sessionId, String key, String expectedValue);

    /**
     * 条件写（CAS，spec 56 §A / T249）：仅当当前 value 与 {@code expectedValue} 相等时以
     * {@code update} 覆写，返回是否成功。{@code expectedValue == null} 表示键当前不存在才写
     * （日翻越首写竞态钉住）。跨实例原子扣减（配额计数等）依赖本方法。
     *
     * <p>默认实现 get+比对+put <b>非原子</b>（check-then-write 竞窗存在）——仅单实例语义；
     * 真原子由各实现覆写：内存 compute、JDBC 条件单语句、Redis Lua（同
     * {@link #deleteIfValueMatches} 先例）。
     */
    default boolean compareAndSwap(String sessionId, String key, String expectedValue, StateEntry update) {
        String current = get(sessionId, key).map(StateEntry::value).orElse(null);
        if (!java.util.Objects.equals(current, expectedValue)) {
            return false;
        }
        put(sessionId, update);
        return true;
    }

    /**
     * impl-35 / spec 13 §stores-6：删除该会话的全部 state 条目（含键集合索引）。幂等——
     * 会话不存在时无操作。默认 no-op（既有实现二进制兼容，由各实现补齐语义）。
     */
    default void deleteSession(String sessionId) {
    }

    /**
     * 键前缀扫描（spec 33 §C / T114 / impl-89）：返回该会话键以 prefix 开头的条目。
     * 默认实现 = getAll 过滤（正确但全量读）；JDBC/Redis 覆写为下推扫描（键条件/集合
     * 侧匹配），供 outbox 等高频前缀键空间消全量读放大。prefix 不得含 LIKE/通配元字符
     *（内部常量约定：webhook outbox 的 {@code outbox.} / {@code dead.}）。
     */
    default Map<String, StateEntry> scanByPrefix(String sessionId, String prefix) {
        Map<String, StateEntry> result = new java.util.LinkedHashMap<>();
        getAll(sessionId).forEach((k, v) -> {
            if (k.startsWith(prefix)) {
                result.put(k, v);
            }
        });
        return result;
    }
}

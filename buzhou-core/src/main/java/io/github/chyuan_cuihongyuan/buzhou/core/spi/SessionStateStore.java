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
     * impl-35 / spec 13 §stores-6：删除该会话的全部 state 条目（含键集合索引）。幂等——
     * 会话不存在时无操作。默认 no-op（既有实现二进制兼容，由各实现补齐语义）。
     */
    default void deleteSession(String sessionId) {
    }
}

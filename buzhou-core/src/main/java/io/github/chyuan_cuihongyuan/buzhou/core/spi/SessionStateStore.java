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
     * 原子 put-if-absent：仅当 {@code (sessionId, entry.key())} 当前不存在时插入，返回是否插入成功。
     * 幂等去重「reserve 一条 pending 记录」的原子语义依赖本方法（spec「崩溃中轮次恢复 / 幂等三件套」）：
     * 多实例 / 重试并发执行同一幂等键时，只有一个 reserve 成功获得执行权，余者命中去重。
     *
     * <p>各后端落地（与 {@link #deleteIfValueMatches} 同口径）：
     * <ul>
     *   <li>JDBC：{@code INSERT} 配唯一索引 / {@code INSERT ... ON CONFLICT DO NOTHING}，按影响行数判定。</li>
     *   <li>Redis：{@code SET key value NX}（或等价 Lua）。</li>
     *   <li>内存：{@code ConcurrentHashMap.computeIfAbsent}（原子）。</li>
     * </ul>
     *
     * <p>默认实现是<b>非原子</b>的 check-then-put 兜底（仅供无并发需求的内存型默认 / 单测占位），
     * 生产后端<b>必须</b>覆写为原子语义。本方法为 additive 默认方法，既有实现源码 / 二进制兼容。
     *
     * @return {@code true} 当且仅当键原本不存在且本次成功插入
     */
    default boolean putIfAbsent(String sessionId, StateEntry entry) {
        if (get(sessionId, entry.key()).isPresent()) {
            return false;
        }
        put(sessionId, entry);
        return true;
    }
}

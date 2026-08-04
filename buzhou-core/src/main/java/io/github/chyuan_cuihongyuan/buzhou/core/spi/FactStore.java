package io.github.chyuan_cuihongyuan.buzhou.core.spi;

/**
 * 事实存取门面（spec 07 事实模型）：建在 {@link SessionStateStore} 上，封装
 * {@code fact.{producer}.{name}} key 命名空间 + JSON 序列化 + ttl 轮次过滤。
 *
 * <p>ttl 语义：剩余轮次内累积注入、过期自动停注。
 * {@code currentTurn - createdTurn < ttl} 视为未过期（createdTurn 当轮即注入）。
 */
public interface FactStore {

    /** 保存事实（序列化 value 为 JSON，复用 StateEntry 的 producer/createdTurn/ttlTurns 字段）。 */
    void save(String sessionId, Fact fact);

    /** 返回会话内未过期的事实（{@code currentTurn - createdTurn < ttl}），按 createdTurn 升序。 */
    java.util.List<Fact> activeFacts(String sessionId, int currentTurn);

    /** 删除指定事实。 */
    void delete(String sessionId, String key);
}

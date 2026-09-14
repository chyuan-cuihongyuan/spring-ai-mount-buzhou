package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.util.function.Supplier;

/**
 * 事务工作单元 SPI——跨多 store 的原子事务边界（store 实现方按能力适配）。
 */
public interface UnitOfWork {

    <T> T executeInTransaction(Supplier<T> work);

    default <T> T executeInTransaction(String sessionId, Supplier<T> work) {
        return executeInTransaction(work);
    }

    /**
     * impl-36 / spec 13 §growth-8：删除该会话的工作单元状态（InMemory 为 per-session
     * 锁对象——不随会话移除即泄漏；JDBC/Redis 事务无 per-session 常驻状态，默认 no-op）。
     * 幂等；由 {@code SessionCleaner} 级联调用。
     */
    default void deleteSession(String sessionId) {
    }
}

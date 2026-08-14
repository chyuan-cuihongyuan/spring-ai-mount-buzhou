package io.github.chyuan_cuihongyuan.buzhou.core.internal.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.UnitOfWork;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class InMemoryUnitOfWork implements UnitOfWork {

    private final ConcurrentHashMap<String, ReentrantLock> locksBySession =
            new ConcurrentHashMap<>();
    private final ReentrantLock globalLock = new ReentrantLock();

    @Override
    public <T> T executeInTransaction(Supplier<T> work) {
        globalLock.lock();
        try {
            return work.get();
        } finally {
            globalLock.unlock();
        }
    }

    @Override
    public <T> T executeInTransaction(String sessionId, Supplier<T> work) {
        ReentrantLock lock = locksBySession.computeIfAbsent(sessionId,
                k -> new ReentrantLock());
        lock.lock();
        try {
            return work.get();
        } finally {
            lock.unlock();
        }
    }

    /**
     * impl-36 / spec 13 §growth-8：per-session 锁对象随会话移除（否则每个曾开过事务的会话
     * 永久驻留一个锁——长跑进程慢性泄漏）。由 SessionCleaner 级联调用；幂等。
     * 诚实边界：仅按会话终结语义移除——若此刻恰有在途事务持有该锁，其 unlock 作用在已
     * 脱离 map 的锁对象上（语义仍正确）；后续该会话若再开新事务会创建新锁。
     */
    @Override
    public void deleteSession(String sessionId) {
        locksBySession.remove(sessionId);
    }

    /** impl-36：该会话是否仍有常驻锁对象（测试可观测——验证锁随会话移除）。 */
    boolean hasSessionLock(String sessionId) {
        return locksBySession.containsKey(sessionId);
    }

    /** impl-36：在册 per-session 锁数（测试与运维可观测）。 */
    int lockCount() {
        return locksBySession.size();
    }
}

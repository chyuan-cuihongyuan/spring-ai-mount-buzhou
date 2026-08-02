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
}

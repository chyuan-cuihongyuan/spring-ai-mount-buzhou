package io.github.chyuan_cuihongyuan.buzhou.core.spi;

import java.util.function.Supplier;

public interface UnitOfWork {

    <T> T executeInTransaction(Supplier<T> work);

    default <T> T executeInTransaction(String sessionId, Supplier<T> work) {
        return executeInTransaction(work);
    }
}

package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.UnitOfWork;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

public class JdbcUnitOfWork implements UnitOfWork {

    private final TransactionTemplate transactionTemplate;

    public JdbcUnitOfWork(TransactionTemplate transactionTemplate) {
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public <T> T executeInTransaction(Supplier<T> work) {
        return transactionTemplate.execute(status -> work.get());
    }
}

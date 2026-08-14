package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * 事务作用域助手（spec 13 §stores-7 / ticket 32）：多语句写（先删后插 / 锁读 + 插入）
 * 的「有则复用、无则自开」语义。
 *
 * <p><b>接线方式</b>：调用方显式开 UoW（{@link JdbcUnitOfWork} 即
 * {@code TransactionTemplate.execute}）时，{@link TransactionSynchronizationManager}
 * 已把事务连接绑定到当前线程——JdbcTemplate 经 DataSourceUtils 自动路由到该连接，
 * 写操作因此天然入其事务（本方法直接执行 work 即完成复用）；未开 UoW 时自开一条
 * 短事务包住整段多语句写，调用方可见行为与既有自动提交一致（一次逻辑写要么全成、
 * 要么全回滚），但崩溃窗口不再产生「删已提交、插未提交」的半份数据。
 *
 * <p>template 为 null（旧构造器兼容路径）时退化为直接执行——保持迁移机制上线前的
 * 既有自动提交行为。
 */
final class JdbcTransactions {

    private JdbcTransactions() {
    }

    /**
     * 在「当前事务（若有）」内执行；无事务时经 template 自开一条短事务。
     *
     * @param template 共享事务模板（与 {@link JdbcUnitOfWork} 同源；可 null = 兼容直通）
     * @param work     需要原子执行的多语句写
     * @param <T>      写操作返回类型
     * @return work 的返回值
     */
    static <T> T inCurrentOrNew(@Nullable TransactionTemplate template, Supplier<T> work) {
        if (template == null || TransactionSynchronizationManager.isActualTransactionActive()) {
            return work.get();
        }
        return template.execute(status -> work.get());
    }
}

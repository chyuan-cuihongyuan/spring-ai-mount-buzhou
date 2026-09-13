package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.StoreLatencyRing;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * 计时连接池 DataSource 装饰器（spec 828 / T1157，HikariCP 池等待指标思想
 * ——acquire 与 query 分离计量）：包装池化 {@link DataSource}（Spring Boot
 * 默认 HikariCP），{@code getConnection} 耗时（含池等待+建连）进
 * {@link StoreLatencyRing}（操作名 getConnection）——「慢在拿连接还是慢在
 * 查询」（与 810 的 store 操作计时正交两层）。其余 DataSource 方法纯委托。
 * 异常照抛、耗时照记（finally——810 同口径）。
 */
public final class TimedDataSource implements DataSource {

    private final DataSource delegate;
    private final StoreLatencyRing ring;

    public TimedDataSource(DataSource delegate, StoreLatencyRing ring) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.ring = Objects.requireNonNull(ring, "ring");
    }

    @Override
    public Connection getConnection() throws SQLException {
        long start = System.nanoTime();
        try {
            return delegate.getConnection();
        } finally {
            ring.record("getConnection", elapsedMillis(start));
        }
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        long start = System.nanoTime();
        try {
            return delegate.getConnection(username, password);
        } finally {
            ring.record("getConnection-user-pass", elapsedMillis(start));
        }
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        return delegate.getLogWriter();
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        delegate.setLogWriter(out);
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        delegate.setLoginTimeout(seconds);
    }

    @Override
    public int getLoginTimeout() throws SQLException {
        return delegate.getLoginTimeout();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        return delegate.unwrap(iface);
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return delegate.isWrapperFor(iface);
    }

    private static long elapsedMillis(long startNanos) {
        return Math.max(0, (System.nanoTime() - startNanos) / 1_000_000);
    }
}

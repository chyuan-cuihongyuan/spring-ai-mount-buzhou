package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.StoreLatencyRing;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 828 / T1158：计时 DataSource 回归——getConnection 计时/异常照记照抛/
 * 其余方法委托/unwrap 透传/fail-fast。
 */
class TimedDataSourceTest {

    /** 可控桩：计数+可抛异常。 */
    private static final class StubDataSource implements DataSource {
        int connections;
        boolean explode;
        PrintWriter logWriter;
        int loginTimeout = -1;
        boolean unwrapCalled;
        boolean wrapperFor = true;

        @Override
        public Connection getConnection() throws SQLException {
            connections++;
            if (explode) {
                throw new SQLException("pool exhausted");
            }
            return null;
        }

        @Override
        public Connection getConnection(String username, String password) {
            connections++;
            return null;
        }

        @Override
        public PrintWriter getLogWriter() {
            return logWriter;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
            this.logWriter = out;
        }

        @Override
        public void setLoginTimeout(int seconds) {
            this.loginTimeout = seconds;
        }

        @Override
        public int getLoginTimeout() {
            return loginTimeout;
        }

        @Override
        public Logger getParentLogger() {
            return Logger.getLogger("stub");
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T unwrap(Class<T> iface) {
            unwrapCalled = true;
            return (T) this;
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return wrapperFor;
        }
    }

    @Test
    void recordsConnectionAcquisitions() throws SQLException {
        StubDataSource stub = new StubDataSource();
        StoreLatencyRing ring = new StoreLatencyRing();
        TimedDataSource timed = new TimedDataSource(stub, ring);

        timed.getConnection();
        timed.getConnection("u", "p");

        assertThat(stub.connections).isEqualTo(2);
        assertThat(ring.stats("getConnection").count()).isEqualTo(1);
        assertThat(ring.stats("getConnection-user-pass").count()).isEqualTo(1);
    }

    @Test
    void recordsLatencyOnExceptionAndRethrows() {
        StubDataSource stub = new StubDataSource();
        stub.explode = true;
        StoreLatencyRing ring = new StoreLatencyRing();
        TimedDataSource timed = new TimedDataSource(stub, ring);

        assertThatThrownBy(timed::getConnection).isInstanceOf(SQLException.class);
        assertThat(ring.stats("getConnection").count()).isEqualTo(1); // 异常也计时
    }

    @Test
    void delegatesOtherMethods() throws SQLException {
        StubDataSource stub = new StubDataSource();
        TimedDataSource timed = new TimedDataSource(stub, new StoreLatencyRing());

        PrintWriter writer = new PrintWriter(System.out);
        timed.setLogWriter(writer);
        timed.setLoginTimeout(30);
        assertThat(timed.getLogWriter()).isSameAs(writer);
        assertThat(timed.getLoginTimeout()).isEqualTo(30);
        assertThat(timed.getParentLogger().getName()).isEqualTo("stub");
        assertThat(timed.isWrapperFor(DataSource.class)).isTrue();
        assertThat(timed.unwrap(DataSource.class)).isSameAs(stub);
        assertThat(stub.unwrapCalled).isTrue();
    }

    @Test
    void failFastOnNulls() {
        assertThatThrownBy(() -> new TimedDataSource(null, new StoreLatencyRing()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new TimedDataSource(new StubDataSource(), null))
                .isInstanceOf(NullPointerException.class);
    }
}

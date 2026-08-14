package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * impl-42 / spec 13 §T68：JDBC 方言缺省自动探测（DatabaseMetaData）——
 * H2 连接探为 H2；不可用连接给显式配置指引。
 */
class DialectDetectTest {

    private javax.sql.DataSource h2DataSource() {
        return new DriverManagerDataSource(
                "jdbc:h2:mem:dialect-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", "");
    }

    @Test
    void detectsH2FromConnectionMetadata() {
        assertThat(Dialect.detect(h2DataSource())).isEqualTo(Dialect.H2);
    }

    @Test
    void unreachableConnectionFailsWithExplicitConfigHint() {
        // 不可达端口（tcp 连不上即失败；H2 mem 会自动建库，测不了失败路径）
        DriverManagerDataSource broken = new DriverManagerDataSource(
                "jdbc:h2:tcp://localhost:1/mem:x", "sa", "");
        assertThatThrownBy(() -> Dialect.detect(broken))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("buzhou.store.jdbc.dialect");
    }

    @Test
    void dialectPropertyDefaultIsAuto() {
        assertThat(new io.github.chyuan_cuihongyuan.buzhou.store.jdbc.config.JdbcStoreProperties(
                null, null).dialect()).isEqualTo("AUTO");
        assertThat(new io.github.chyuan_cuihongyuan.buzhou.store.jdbc.config.JdbcStoreProperties(
                "mysql", null).dialect()).isEqualTo("MYSQL");
    }
}

package io.github.chyuan_cuihongyuan.buzhou.store.jdbc.config;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JDBC store 装配测试（ticket 22）：store.type=jdbc + H2 内嵌 DataSource（hermetic，无 Docker）。
 */
class BuzhouJdbcStoreAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(BuzhouJdbcStoreAutoConfiguration.class))
            .withBean(DataSource.class, () -> new EmbeddedDatabaseBuilder()
                    .setType(EmbeddedDatabaseType.H2).build());

    @Test
    void jdbcStoreAssemblesOnJdbcType() {
        runner.withPropertyValues("buzhou.store.type=jdbc").run(ctx -> {
            assertThat(ctx).hasSingleBean(BuzhouStores.class);
            assertThat(ctx.getBean(BuzhouStores.class).messageStore().getClass().getSimpleName())
                    .contains("Jdbc");
        });
    }

    @Test
    void notActiveForMemoryType() {
        runner.withPropertyValues("buzhou.store.type=memory")
                .run(ctx -> assertThat(ctx).doesNotHaveBean(BuzhouStores.class));
    }
}

package io.github.chyuan_cuihongyuan.buzhou.store.jdbc.config;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.store.jdbc.Dialect;
import io.github.chyuan_cuihongyuan.buzhou.store.jdbc.JdbcBuzhouStores;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import javax.sql.DataSource;

/**
 * JDBC store 自装配（spec 08 / 09 / ticket 22）。
 *
 * <p>当 {@code buzhou.store.type=jdbc} 时由 {@link JdbcBuzhouStores#create} 用容器内
 * {@link DataSource} 建库（按 {@code buzhou.store.jdbc.dialect} 选 schema 脚本）产出
 * {@link BuzhouStores}，替换 core 的内存默认。需要容器内已有 {@link DataSource} bean
 * （由 Spring Boot 的 DataSource 自动装配或业务侧提供）。
 */
@AutoConfiguration
@ConditionalOnProperty(prefix = "buzhou.store", name = "type", havingValue = "jdbc")
@EnableConfigurationProperties(JdbcStoreProperties.class)
public class BuzhouJdbcStoreAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public BuzhouStores buzhouStores(DataSource dataSource, JdbcStoreProperties props) {
        return JdbcBuzhouStores.create(dataSource, Dialect.valueOf(props.dialect()));
    }
}

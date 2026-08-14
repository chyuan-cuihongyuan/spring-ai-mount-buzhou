package io.github.chyuan_cuihongyuan.buzhou.skill.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.skill.store.AbstractSkillStoreContractTest;
import io.github.chyuan_cuihongyuan.buzhou.skill.store.SkillStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * impl-51：{@link JdbcSkillStore} 契约测试（H2 内存库——真实 SQL 路径，含自含 DDL）。
 */
class JdbcSkillStoreContractTest extends AbstractSkillStoreContractTest {

    private final SkillStore store;

    JdbcSkillStoreContractTest() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:skill-store-test-" + System.nanoTime() + ";DB_CLOSE_DELAY=-1",
                "sa", "");
        dataSource.setDriverClassName("org.h2.Driver");
        store = new JdbcSkillStore(new JdbcTemplate(dataSource));
    }

    @Override
    protected SkillStore store() {
        return store;
    }
}

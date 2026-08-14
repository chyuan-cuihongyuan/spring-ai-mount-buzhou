package io.github.chyuan_cuihongyuan.buzhou.store.jdbc;

import io.github.chyuan_cuihongyuan.buzhou.core.contract.AbstractBuzhouStoresContractTest;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class H2StoresContractTest extends AbstractBuzhouStoresContractTest {

    private final BuzhouStores stores;

    H2StoresContractTest() {
        JdbcDataSource dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:contract-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        stores = JdbcBuzhouStores.createWithRecovery(dataSource, Dialect.H2).stores();
    }

    @Override
    protected BuzhouStores stores() {
        return stores;
    }

    @Override
    protected void cleanUp() {
    }

    @Test
    void unitOfWorkRollsBackAllWritesOnFailure() {
        String sessionId = "rollback-" + UUID.randomUUID();
        assertThatThrownBy(() -> stores().unitOfWork().executeInTransaction(sessionId, () -> {
            stores().messageStore().append(sessionId, List.of(msg(sessionId, 1, 0, Role.USER)));
            stores().sessionStateStore().put(sessionId,
                    new StateEntry("fact.x", "v", "hook", 1, null, Instant.now()));
            throw new IllegalStateException("simulated mid-turn failure");
        })).isInstanceOf(IllegalStateException.class);

        assertThat(stores().messageStore().load(sessionId)).isEmpty();
        assertThat(stores().sessionStateStore().getAll(sessionId)).isEmpty();
    }

    @Test
    void unitOfWorkCommitsAtomicallyOnSuccess() {
        String sessionId = "commit-" + UUID.randomUUID();
        stores().unitOfWork().executeInTransaction(sessionId, () -> {
            stores().messageStore().append(sessionId, List.of(msg(sessionId, 1, 0, Role.USER)));
            stores().sessionStateStore().put(sessionId,
                    new StateEntry("fact.x", "v", "hook", 1, null, Instant.now()));
            return null;
        });

        assertThat(stores().messageStore().load(sessionId)).hasSize(1);
        assertThat(stores().sessionStateStore().getAll(sessionId)).hasSize(1);
    }
}

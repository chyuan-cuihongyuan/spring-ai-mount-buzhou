package io.github.chyuan_cuihongyuan.buzhou.store.redis;

import com.github.fppt.jedismock.RedisServer;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.StateEntry;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 13 §stores-7 / ticket 32：RedisUnitOfWork 连接池化契约（hermetic，jedis-mock）——
 * 事务连接从池借出/归还复用（上限可配），事务原子提交/回滚语义与直连路径一致。
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RedisPooledUnitOfWorkTest {

    /** 池上限 1：串行化借出，验证借还路径不死锁、事务间互不串味。 */
    private static final int POOL_MAX_SIZE = 1;

    private static final int CONCURRENT_TRANSACTIONS = 8;

    private RedisServer server;
    private RedisClient client;
    private BuzhouStores stores;
    private StatefulRedisConnection<String, String> adminConn;

    @BeforeAll
    void setUp() throws IOException {
        server = new RedisServer(0);
        server.start();
        client = RedisClient.create("redis://" + server.getHost() + ":" + server.getBindPort());
        stores = RedisBuzhouStores.createPooled(client, "buzhou:", Duration.ZERO,
                POOL_MAX_SIZE, null);
        adminConn = client.connect();
    }

    @AfterAll
    void tearDown() throws IOException {
        adminConn.close();
        client.shutdown();
        server.stop();
    }

    @BeforeEach
    void flushBetweenTests() {
        adminConn.sync().flushdb();
    }

    @Test
    void shouldCommitAndReusePooledConnections_whenTransactionsRunSequentially() {
        String sessionId = "pool-seq-" + UUID.randomUUID();
        // 连跑多笔事务：同一池化连接反复借出归还（池上限 1 → 必然复用）
        for (int i = 0; i < CONCURRENT_TRANSACTIONS; i++) {
            int seq = i;
            stores.unitOfWork().executeInTransaction(sessionId, () -> {
                stores.messageStore().append(sessionId, List.of(msg(sessionId, seq, 0)));
                stores.sessionStateStore().put(sessionId,
                        new StateEntry("fact." + seq, "v", "hook", 1, null, Instant.now()));
                return null;
            });
        }

        assertThat(stores.messageStore().load(sessionId)).hasSize(CONCURRENT_TRANSACTIONS);
        assertThat(stores.sessionStateStore().getAll(sessionId)).hasSize(CONCURRENT_TRANSACTIONS);
    }

    @Test
    void shouldCommitAllTransactions_whenPoolSharedByConcurrentWorkers() throws Exception {
        String sessionId = "pool-conc-" + UUID.randomUUID();
        try (ExecutorService pool = Executors.newFixedThreadPool(CONCURRENT_TRANSACTIONS)) {
            Future<?>[] futures = new Future<?>[CONCURRENT_TRANSACTIONS];
            for (int i = 0; i < CONCURRENT_TRANSACTIONS; i++) {
                int seq = i;
                futures[i] = pool.submit(() -> {
                    stores.unitOfWork().executeInTransaction(sessionId, () -> {
                        stores.messageStore().append(sessionId, List.of(msg(sessionId, seq, 0)));
                        return null;
                    });
                    return null;
                });
            }
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        }
        assertThat(stores.messageStore().load(sessionId)).hasSize(CONCURRENT_TRANSACTIONS);
    }

    @Test
    void shouldRollbackPooledTransaction_whenWorkFailsMidway() {
        String sessionId = "pool-rollback-" + UUID.randomUUID();
        assertThatThrownBy(() -> stores.unitOfWork().executeInTransaction(sessionId, () -> {
            stores.messageStore().append(sessionId, List.of(msg(sessionId, 1, 0)));
            stores.sessionStateStore().put(sessionId,
                    new StateEntry("fact.x", "v", "hook", 1, null, Instant.now()));
            throw new IllegalStateException("simulated mid-transaction failure");
        })).isInstanceOf(IllegalStateException.class);

        // 回滚后连接归还/销毁路径不污染后续事务：紧接着的正常事务照常提交
        assertThat(stores.messageStore().load(sessionId)).isEmpty();
        stores.unitOfWork().executeInTransaction(sessionId, () -> {
            stores.messageStore().append(sessionId, List.of(msg(sessionId, 1, 0)));
            return null;
        });
        assertThat(stores.messageStore().load(sessionId)).hasSize(1);
    }

    private BuzhouMessage msg(String sessionId, int turnSeq, int seqInTurn) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turnSeq, seqInTurn,
                Role.USER, "content-" + turnSeq, List.of(), null, null, null, Map.of(), Instant.now());
    }
}

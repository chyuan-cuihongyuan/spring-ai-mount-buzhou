package io.github.chyuan_cuihongyuan.buzhou.memory;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.message.BuzhouMessage;
import io.github.chyuan_cuihongyuan.buzhou.core.message.Role;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ForkStatsTest {

    private static BuzhouMessage user(String sessionId, int turn, String content) {
        return new BuzhouMessage(UUID.randomUUID().toString(), sessionId, turn, 0, Role.USER,
                content, List.of(), null, null, null, Map.of(), Instant.now());
    }

    @Test
    void freshForksHaveZeroCounts() {
        SessionForks forks = new SessionForks(Buzhou.inMemoryStores().messageStore());

        assertThat(forks.stats()).isEqualTo(new SessionForks.ForkStats(0, 0));
    }

    @Test
    void forkCountsForksAndCopiedMessages() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        SessionForks forks = new SessionForks(stores.messageStore());
        String sid = "origin";
        for (int turn = 1; turn <= 5; turn++) {
            stores.messageStore().append(sid, List.of(user(sid, turn, "第" + turn + "轮")));
        }

        String forked = forks.forkFrom(sid, 3, "fork-1");

        assertThat(forked).isEqualTo("fork-1");
        assertThat(forks.stats().forksCreated()).isEqualTo(1);
        assertThat(forks.stats().messagesCopied()).isEqualTo(3);
        // 原会话不动（隔离分叉语义回归）
        assertThat(stores.messageStore().load(sid)).hasSize(5);
    }

    @Test
    void repeatedForksAccumulate() {
        var stores = Buzhou.inMemoryStores();
        SessionForks forks = new SessionForks(stores.messageStore());
        String sid = "origin";
        for (int turn = 1; turn <= 4; turn++) {
            stores.messageStore().append(sid, List.of(user(sid, turn, "第" + turn + "轮")));
        }

        forks.forkFrom(sid, 2, "fork-a");
        forks.forkFrom(sid, 4, "fork-b");

        SessionForks.ForkStats stats = forks.stats();
        assertThat(stats.forksCreated()).isEqualTo(2);
        assertThat(stats.messagesCopied()).isEqualTo(6);
    }
}

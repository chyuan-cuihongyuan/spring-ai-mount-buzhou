package io.github.chyuan_cuihongyuan.buzhou.mcp;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.ToolSetSpec;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.Transport;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class DbToolSetProviderTest {

    private static ToolSetSpec spec(String name) {
        return new ToolSetSpec(name, Transport.STDIO, "cmd-" + name,
                Map.of(), null, null, Set.of());
    }

    @Test
    void writePushesChangeImmediately() throws Exception {
        InMemoryToolSetSpecStore store = new InMemoryToolSetSpecStore();
        store.replaceAll(List.of(spec("a")));
        DbToolSetProvider provider = new DbToolSetProvider(store, Duration.ofSeconds(60));
        try {
            assertThat(provider.currentToolSets()).extracting(ToolSetSpec::name).containsExactly("a");

            CountDownLatch fired = new CountDownLatch(1);
            provider.addChangeListener(fired::countDown);
            store.replaceAll(List.of(spec("a"), spec("b")));

            assertThat(fired.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(provider.currentToolSets()).extracting(ToolSetSpec::name)
                    .containsExactlyInAnyOrder("a", "b");
        } finally {
            provider.close();
        }
    }

    @Test
    void pollingDetectsExternalStoreChange() {
        // 不经 InMemoryToolSetSpecStore 的写通知（模拟纯 DB 改配），靠轮询发现
        AtomicInteger version = new AtomicInteger(0);
        ToolSetSpecStore store = () -> version.get() == 0 ? List.of(spec("a"))
                : List.of(spec("a"), spec("b"));
        DbToolSetProvider provider = new DbToolSetProvider(store, Duration.ofMillis(50));
        try {
            assertThat(provider.currentToolSets()).hasSize(1);
            CountDownLatch fired = new CountDownLatch(1);
            provider.addChangeListener(fired::countDown);

            version.set(1);
            FakeMcp.await("polling picks up change", 3000, () -> fired.getCount() == 0);
            assertThat(provider.currentToolSets()).hasSize(2);
        } finally {
            provider.close();
        }
    }

    @Test
    void unchangedSnapshotDoesNotFire() throws Exception {
        InMemoryToolSetSpecStore store = new InMemoryToolSetSpecStore();
        store.replaceAll(List.of(spec("a")));
        DbToolSetProvider provider = new DbToolSetProvider(store, Duration.ofMillis(50));
        try {
            AtomicInteger fires = new AtomicInteger();
            provider.addChangeListener(fires::incrementAndGet);
            Thread.sleep(300);   // 数轮轮询
            assertThat(fires.get()).isZero();
        } finally {
            provider.close();
        }
    }
}

package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SpillStoreTest {

    @TempDir
    Path rootDir;

    private DiskSpillStore store() {
        return new DiskSpillStore(rootDir);
    }

    @Test
    void uriRoundTripsAndValidatesCharset() {
        SpillUri uri = new SpillUri("ops-agent", "sess-1", "tc-9");
        assertThat(SpillUri.parse(uri.toString())).isEqualTo(uri);
        assertThatThrownBy(() -> new SpillUri("bad/name", "s", "t"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SpillUri("..", "s", "t"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void storeLoadAndDuplicateConflict() {
        DiskSpillStore store = store();
        SpillUri uri = new SpillUri("agent", "s1", "tc-1");
        store.store(SpillEntry.of(uri, "x".repeat(5000)), 2048);

        assertThat(store.exists(uri)).isTrue();
        assertThat(store.load(uri)).contains("x".repeat(5000));
        assertThatThrownBy(() -> store.store(SpillEntry.of(uri, "again"), 2048))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void readRangeThreeModes() {
        DiskSpillStore store = store();
        SpillUri uri = new SpillUri("agent", "s1", "tc-2");
        store.store(SpillEntry.of(uri, """
                [{"id":1,"status":"OK"},{"id":2,"status":"FAIL"},{"id":3,"status":"OK"}]
                """.strip()), 2048);

        RangeReadResult page = store.readRange(uri, RangeReadRequest.page(null, 2));
        assertThat(page.truncated()).isTrue();
        assertThat(page.content()).contains("\"totalCount\":3").contains("\"id\":1");
        assertThat(page.nextCursor()).isNotBlank();

        RangeReadResult page2 = store.readRange(uri, RangeReadRequest.page(page.nextCursor(), 2));
        assertThat(page2.truncated()).isFalse();
        assertThat(page2.content()).contains("\"id\":3");

        RangeReadResult json = store.readRange(uri, RangeReadRequest.json("$[1].status"));
        assertThat(json.content()).isEqualTo("FAIL");

        RangeReadResult bytes = store.readRange(uri, RangeReadRequest.bytes(0, 10));
        assertThat(bytes.content()).hasSize(10);
        assertThat(bytes.truncated()).isTrue();
    }

    @Test
    void jsonListPreviewIsFirstNWithCount() {
        String preview = RangeReadEngine.previewOf("[1,2,3,4,5]", 2048, 2);
        assertThat(preview).contains("\"totalCount\":5").contains("\"truncated\":true")
                .contains("1").contains("2");
    }

    @Test
    void lifecycleCleanupKeepsLinkedDeletesUnlinked() {
        DiskSpillStore store = store();
        SpillUri unlinked = new SpillUri("agent", "s1", "tc-a");
        SpillUri linked = new SpillUri("agent", "s1", "tc-b");
        store.store(SpillEntry.of(unlinked, "data-a"), 2048);
        store.store(SpillEntry.of(linked, "data-b"), 2048);
        store.markLinked(linked);

        assertThat(store.deleteBySession("agent", "s1")).isEqualTo(2);
        assertThat(store.exists(unlinked)).isFalse();
        assertThat(store.exists(linked)).isFalse();
    }

    @Test
    void ttlSweeperDeletesExpiredUnlinkedOnly() throws Exception {
        DiskSpillStore store = store();
        SpillUri old = new SpillUri("agent", "s1", "tc-old");
        SpillUri linked = new SpillUri("agent", "s1", "tc-linked");
        store.store(SpillEntry.of(old, "old-data"), 2048);
        store.store(SpillEntry.of(linked, "linked-data"), 2048);
        store.markLinked(linked);

        int deleted = store.deleteExpired(Instant.now().plus(Duration.ofDays(8)),
                Duration.ofDays(7));
        assertThat(deleted).isEqualTo(1);
        assertThat(store.exists(old)).isFalse();
        assertThat(store.exists(linked)).isTrue();
    }

    @Test
    void concurrentSpillsWithDistinctToolCallIdsDoNotConflict() throws Exception {
        DiskSpillStore store = store();
        int count = 20;
        CountDownLatch latch = new CountDownLatch(count);
        List<Throwable> failures = java.util.Collections.synchronizedList(new ArrayList<>());
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < count; i++) {
                final int n = i;
                executor.submit(() -> {
                    try {
                        store.store(SpillEntry.of(
                                new SpillUri("agent", "s1", "tc-" + n), "data-" + n), 2048);
                    } catch (Throwable t) {
                        failures.add(t);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            latch.await();
        }
        assertThat(failures).isEmpty();
        for (int i = 0; i < count; i++) {
            assertThat(store.exists(new SpillUri("agent", "s1", "tc-" + i))).isTrue();
        }
    }
}

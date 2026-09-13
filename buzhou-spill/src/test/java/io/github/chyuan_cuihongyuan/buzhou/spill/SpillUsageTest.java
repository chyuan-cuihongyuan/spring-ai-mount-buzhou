package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class SpillUsageTest {

    @TempDir
    Path root;

    private DiskSpillStore store() {
        return new DiskSpillStore(root);
    }

    @Test
    void emptyStoreIsZeroBytesZeroEntries() {
        SpillUsage usage = store().usage();

        assertThat(usage.totalBytes()).isZero();
        assertThat(usage.entryCount()).isZero();
    }

    @Test
    void storesAreCountedWithByteTotal() {
        DiskSpillStore store = store();
        store.store(new SpillEntry(new SpillUri("agent", "s1", "tc-1"),
                "hello-world", "text/plain", "hello-world".length(), Instant.now()), 4);
        store.store(new SpillEntry(new SpillUri("agent", "s1", "tc-2"),
                "abc", "text/plain", 3, Instant.now()), 2);

        SpillUsage usage = store.usage();
        assertThat(usage.entryCount()).isEqualTo(2);
        assertThat(usage.totalBytes()).isEqualTo("hello-world".length() + "abc".length());
    }

    @Test
    void deleteReducesUsage() {
        DiskSpillStore store = store();
        SpillUri uri = new SpillUri("agent", "s1", "tc-1");
        store.store(new SpillEntry(uri, "hello-world", "text/plain", 11, Instant.now()), 4);
        assertThat(store.usage().entryCount()).isEqualTo(1);

        store.delete(uri);

        assertThat(store.usage().entryCount()).isZero();
        assertThat(store.usage().totalBytes()).isZero();
    }
}

package io.github.chyuan_cuihongyuan.buzhou.core.cleanup;

import io.github.chyuan_cuihongyuan.buzhou.core.Buzhou;
import io.github.chyuan_cuihongyuan.buzhou.core.spi.BuzhouStores;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CleanupStatsTest {

    @Test
    void freshCleanerHasZeroCounts() {
        SessionCleaner cleaner = new SessionCleaner(Buzhou.inMemoryStores());

        SessionCleaner.CleanupStats stats = cleaner.cleanupStats();
        assertThat(stats.deleteCalls()).isZero();
        assertThat(stats.cleanedTargets()).isZero();
        assertThat(stats.failedTargets()).isZero();
        assertThat(stats.failuresByTarget()).isEmpty();
    }

    @Test
    void singleSessionCleanupCountsTargets() {
        SessionCleaner cleaner = new SessionCleaner(Buzhou.inMemoryStores());

        SessionCleaner.CleanupStats before = cleaner.cleanupStats();
        cleaner.deleteSession("s-1");

        SessionCleaner.CleanupStats after = cleaner.cleanupStats();
        assertThat(after.deleteCalls()).isEqualTo(before.deleteCalls() + 1);
        assertThat(after.cleanedTargets()).isGreaterThan(before.cleanedTargets());
        assertThat(after.failedTargets()).isZero();
    }

    @Test
    void failingContributorCountedPerTarget() {
        BuzhouStores stores = Buzhou.inMemoryStores();
        SessionCleaner cleaner = new SessionCleaner(stores)
                .withContributor("bad-contributor", sessionId -> {
                    throw new IllegalStateException("贡献者故障");
                });

        cleaner.deleteSession("s-1");

        SessionCleaner.CleanupStats stats = cleaner.cleanupStats();
        assertThat(stats.failedTargets()).isEqualTo(1);
        assertThat(stats.failuresByTarget()).containsEntry("bad-contributor", 1L);
        assertThat(stats.cleanedTargets()).isGreaterThan(0);
    }

    @Test
    void multiSessionAccumulates() {
        SessionCleaner cleaner = new SessionCleaner(Buzhou.inMemoryStores());

        cleaner.deleteSession("s-a");
        cleaner.deleteSession("s-b");

        assertThat(cleaner.cleanupStats().deleteCalls()).isEqualTo(2);
    }
}

package io.github.chyuan_cuihongyuan.buzhou.core.token;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TableContextWindowResolverStatsTest {

    @Test
    void overrideHitCountedWithConfiguredWindow() {
        TableContextWindowResolver resolver = new TableContextWindowResolver(
                Map.of("my-model", 500_000));

        assertThat(resolver.resolveWindow("my-model")).isEqualTo(500_000);

        TableContextWindowResolver.WindowResolutionStats stats = resolver.stats();
        assertThat(stats.overrideHits()).isEqualTo(1);
        assertThat(stats.builtInHits()).isZero();
        assertThat(stats.fallbackHits()).isZero();
        assertThat(stats.resolvedWindows().get("my-model")).isEqualTo(500_000);
    }

    @Test
    void builtInPrefixHitCountedCaseInsensitive() {
        TableContextWindowResolver resolver = new TableContextWindowResolver(Map.of());

        assertThat(resolver.resolveWindow("GPT-4-turbo-preview")).isEqualTo(128_000);

        TableContextWindowResolver.WindowResolutionStats stats = resolver.stats();
        assertThat(stats.builtInHits()).isEqualTo(1);
        assertThat(stats.fallbackHits()).isZero();
        assertThat(stats.resolvedWindows().get("GPT-4-turbo-preview")).isEqualTo(128_000);
    }

    @Test
    void unknownModelFallsBackCountedAndWarnedOncePerModel() {
        TableContextWindowResolver resolver = new TableContextWindowResolver(Map.of());

        assertThat(resolver.resolveWindow("mystery-model")).isEqualTo(32_768);
        assertThat(resolver.resolveWindow("mystery-model")).isEqualTo(32_768);

        TableContextWindowResolver.WindowResolutionStats stats = resolver.stats();
        assertThat(stats.fallbackHits()).isEqualTo(2);
        assertThat(stats.resolvedWindows().get("mystery-model")).isEqualTo(32_768);
    }

    @Test
    void nullModelFallsBackCounted() {
        TableContextWindowResolver resolver = new TableContextWindowResolver(Map.of());

        assertThat(resolver.resolveWindow(null)).isEqualTo(32_768);
        assertThat(resolver.stats().fallbackHits()).isEqualTo(1);
    }

    @Test
    void threePathsConserveToTotalResolves() {
        TableContextWindowResolver resolver = new TableContextWindowResolver(
                Map.of("my-model", 500_000));

        resolver.resolveWindow("my-model");
        resolver.resolveWindow("gpt-4");
        resolver.resolveWindow("mystery");
        resolver.resolveWindow(null);

        TableContextWindowResolver.WindowResolutionStats stats = resolver.stats();
        assertThat(stats.total()).isEqualTo(4);
        assertThat(stats.resolvedWindows()).containsEntry("my-model", 500_000)
                .containsEntry("gpt-4", 128_000)
                .containsEntry("mystery", 32_768);
    }

    @Test
    void snapshotIsImmutable() {
        TableContextWindowResolver resolver = new TableContextWindowResolver(Map.of());
        resolver.resolveWindow("gpt-4");

        var stats = resolver.stats();
        assertThatThrownBy(() -> stats.resolvedWindows().put("x", 1))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}

package io.github.chyuan_cuihongyuan.buzhou.core.prompt;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PromptResolutionStatsTest {

    @Test
    void freshRegistryHasZeroCounts() {
        InMemoryPromptRegistry registry = new InMemoryPromptRegistry();

        assertThat(registry.resolutionStats())
                .isEqualTo(new InMemoryPromptRegistry.PromptResolutionStats(0, 0, 0));
    }

    @Test
    void latestResolveCountsHit() {
        InMemoryPromptRegistry registry = new InMemoryPromptRegistry();
        registry.publish("greeting", "你好", "v1");

        assertThat(registry.resolve("greeting")).isPresent();

        InMemoryPromptRegistry.PromptResolutionStats stats = registry.resolutionStats();
        assertThat(stats.attempts()).isEqualTo(1);
        assertThat(stats.hits()).isEqualTo(1);
        assertThat(stats.misses()).isZero();
    }

    @Test
    void labelResolveHitAndMissCountedSeparately() {
        InMemoryPromptRegistry registry = new InMemoryPromptRegistry();
        registry.publish("greeting", "你好", "v1");
        registry.label("greeting", "prod", 1);

        assertThat(registry.resolve("greeting", "prod")).isPresent();
        assertThat(registry.resolve("greeting", "staging")).isEmpty();

        InMemoryPromptRegistry.PromptResolutionStats stats = registry.resolutionStats();
        assertThat(stats.attempts()).isEqualTo(2);
        assertThat(stats.hits()).isEqualTo(1);
        assertThat(stats.misses()).isEqualTo(1);
    }

    @Test
    void versionResolveUnknownVersionCountedMiss() {
        InMemoryPromptRegistry registry = new InMemoryPromptRegistry();
        registry.publish("greeting", "你好", "v1");

        assertThat(registry.resolveVersion("greeting", 99)).isEmpty();

        InMemoryPromptRegistry.PromptResolutionStats stats = registry.resolutionStats();
        assertThat(stats.misses()).isEqualTo(1);
        assertThat(stats.hits()).isZero();
    }

    @Test
    void unknownPromptCountsMiss() {
        InMemoryPromptRegistry registry = new InMemoryPromptRegistry();

        assertThat(registry.resolve("ghost").isEmpty()).isTrue();

        InMemoryPromptRegistry.PromptResolutionStats stats = registry.resolutionStats();
        assertThat(stats.misses()).isEqualTo(1);
        assertThat(stats.attempts()).isEqualTo(1);
    }

    @Test
    void conservationHoldsAcrossMixedResolutions() {
        InMemoryPromptRegistry registry = new InMemoryPromptRegistry();
        registry.publish("greeting", "你好", "v1");

        registry.resolve("greeting");
        registry.resolve("greeting", "prod");
        registry.resolve("greeting", "ghost-label");
        registry.resolveVersion("greeting", 99);
        registry.resolve("ghost");

        InMemoryPromptRegistry.PromptResolutionStats stats = registry.resolutionStats();
        assertThat(stats.attempts()).isEqualTo(5);
        assertThat(stats.hits() + stats.misses()).isEqualTo(stats.attempts());
    }
}

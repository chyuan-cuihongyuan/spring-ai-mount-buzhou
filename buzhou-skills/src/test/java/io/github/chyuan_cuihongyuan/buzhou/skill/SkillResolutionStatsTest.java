package io.github.chyuan_cuihongyuan.buzhou.skill;

import io.github.chyuan_cuihongyuan.buzhou.skill.classpath.ClasspathSkillScanner;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SkillResolutionStatsTest {

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static DefaultSkillRegistry registry() {
        return new DefaultSkillRegistry(
                (Map) new ClasspathSkillScanner().scan(), null, null, false, 64);
    }

    @Test
    void freshRegistryHasZeroCounts() {
        assertThat(registry().resolutionStats())
                .isEqualTo(new SkillResolutionStats(0, 0, 0));
    }

    @Test
    void knownSkillCountsResolved() {
        DefaultSkillRegistry registry = registry();
        var known = registry.listAllFor("app", "agent");
        assertThat(known).isNotEmpty(); // 测试 classpath 自带技能
        String name = known.get(0).name();

        assertThat(registry.load("app", "agent", name)).isPresent();

        SkillResolutionStats stats = registry.resolutionStats();
        assertThat(stats.loads()).isEqualTo(1);
        assertThat(stats.resolved()).isEqualTo(1);
        assertThat(stats.notFound()).isZero();
    }

    @Test
    void hallucinatedNameCountsNotFound() {
        DefaultSkillRegistry registry = registry();

        assertThat(registry.load("app", "agent", "no-such-skill-xyz")).isEmpty();

        SkillResolutionStats stats = registry.resolutionStats();
        assertThat(stats.loads()).isEqualTo(1);
        assertThat(stats.notFound()).isEqualTo(1);
        assertThat(stats.resolved()).isZero();
    }

    @Test
    void conservationHoldsAcrossMixedLoads() {
        DefaultSkillRegistry registry = registry();
        var known = registry.listAllFor("app", "agent");
        String name = known.get(0).name();

        registry.load("app", "agent", name);
        registry.load("app", "agent", "ghost-one");
        registry.load("app", "agent", name);
        registry.load("app", "agent", "ghost-two");

        SkillResolutionStats stats = registry.resolutionStats();
        assertThat(stats.loads()).isEqualTo(4);
        assertThat(stats.resolved() + stats.notFound()).isEqualTo(stats.loads());
        assertThat(stats.resolved()).isEqualTo(2);
        assertThat(stats.notFound()).isEqualTo(2);
    }

    @Test
    void catalogEnumerationDoesNotPolluteCounts() {
        DefaultSkillRegistry registry = registry();

        registry.listFor("app", "agent");
        registry.listAllFor("app", "agent");
        registry.isVisibleFor("app", "agent", "x");

        assertThat(registry.resolutionStats().loads()).isZero();
    }
}

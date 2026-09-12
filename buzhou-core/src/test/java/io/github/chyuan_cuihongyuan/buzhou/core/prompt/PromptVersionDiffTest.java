package io.github.chyuan_cuihongyuan.buzhou.core.prompt;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 534 / T821：版本行级 diff——同行保留/增删行分类与计数、净变化、
 * LCS 最小性（最优对齐）、异名 fail-fast、注册表组合。
 */
class PromptVersionDiffTest {

    private static PromptVersion version(int v, String body) {
        return new PromptVersion("greeting", v, body, "", Instant.EPOCH);
    }

    @Test
    void minimalChangeClassification() {
        var diff = PromptVersionDiff.diff(
                version(1, "你好\n世界"),
                version(2, "你好\n美丽的世界\n！"));
        // LCS 最小对齐：删"世界"、增"美丽的世界"/"！"
        assertThat(diff.lines()).extracting(PromptVersionDiff.DiffLine::context)
                .containsExactly("equal", "delete", "insert", "insert");
        assertThat(diff.added()).isEqualTo(2);
        assertThat(diff.removed()).isEqualTo(1);
        assertThat(diff.net()).isEqualTo(1);
    }

    @Test
    void identicalBodiesProduceAllEqual() {
        var diff = PromptVersionDiff.diff(version(1, "a\nb"), version(2, "a\nb"));
        assertThat(diff.added()).isZero();
        assertThat(diff.removed()).isZero();
        assertThat(diff.lines()).allSatisfy(l -> assertThat(l.context()).isEqualTo("equal"));
    }

    @Test
    void mismatchedNamesFailFast() {
        var v1 = new PromptVersion("a", 1, "x", "", Instant.EPOCH);
        var v2 = new PromptVersion("b", 1, "x", "", Instant.EPOCH);
        assertThatThrownBy(() -> PromptVersionDiff.diff(v1, v2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("版本名不一致");
    }

    @Test
    void composesWithRegistryVersions() {
        InMemoryPromptRegistry registry = new InMemoryPromptRegistry();
        registry.publish("p", "第一行\n第二行", "v1");
        registry.publish("p", "第一行\n第二行改", "v2");
        var versions = registry.versions("p");
        var diff = PromptVersionDiff.diff(versions.get(0), versions.get(1));
        assertThat(diff.fromVersion()).isEqualTo(1);
        assertThat(diff.toVersion()).isEqualTo(2);
        assertThat(diff.removed()).isEqualTo(1);
        assertThat(diff.added()).isEqualTo(1);
    }
}

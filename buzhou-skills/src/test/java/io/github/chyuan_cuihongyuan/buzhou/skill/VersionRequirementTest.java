package io.github.chyuan_cuihongyuan.buzhou.skill;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2039 / T3182：版本要求合同——CARET（含 0.x/0.0.x 特例）、TILDE
 * 次锁定、GTE/GT、EXACT、ANY、prerelease 低段、缺段补 0、畸形
 * fail-fast。
 */
class VersionRequirementTest {

    @Test
    void caretOnStableMajorShouldLockMajorOnly() {
        VersionRequirement req = VersionRequirement.parse("^1.2.3");
        assertThat(req.satisfies("1.2.3")).isTrue();
        assertThat(req.satisfies("1.9.9")).isTrue();   // 同主内兼容
        assertThat(req.satisfies("2.0.0")).isFalse();  // 跨主不兼容
        assertThat(req.satisfies("1.2.2")).isFalse();  // 低于下界
    }

    @Test
    void caretOnZeroMinorShouldLockMinor() {
        VersionRequirement req = VersionRequirement.parse("^0.2.3");
        assertThat(req.satisfies("0.2.9")).isTrue();
        assertThat(req.satisfies("0.3.0")).isFalse(); // 0.x 主次锁定——次也不跨
    }

    @Test
    void caretOnZeroZeroShouldLockPatch() {
        VersionRequirement req = VersionRequirement.parse("^0.0.3");
        assertThat(req.satisfies("0.0.3")).isTrue();
        assertThat(req.satisfies("0.0.4")).isFalse(); // 0.0.x 补丁锁定
    }

    @Test
    void tildeShouldLockMinorVersion() {
        VersionRequirement req = VersionRequirement.parse("~1.2.3");
        assertThat(req.satisfies("1.2.3")).isTrue();
        assertThat(req.satisfies("1.2.99")).isTrue();  // 同次内
        assertThat(req.satisfies("1.3.0")).isFalse();  // 跨次
        assertThat(req.satisfies("1.2.2")).isFalse();
    }

    @Test
    void gteAndGtShouldRespectBoundaryInclusion() {
        assertThat(VersionRequirement.parse(">=1.0").satisfies("1.0.0")).isTrue();  // 含
        assertThat(VersionRequirement.parse(">=1.0").satisfies("0.9.9")).isFalse();
        assertThat(VersionRequirement.parse(">1.0").satisfies("1.0.0")).isFalse(); // 不含
        assertThat(VersionRequirement.parse(">1.0").satisfies("1.0.1")).isTrue();
    }

    @Test
    void exactAndAnyShouldMatchPreciselyOrEverything() {
        assertThat(VersionRequirement.parse("1.4.2").satisfies("1.4.2")).isTrue();
        assertThat(VersionRequirement.parse("1.4.2").satisfies("1.4.3")).isFalse();
        assertThat(VersionRequirement.parse("*").satisfies("0.0.1")).isTrue();
        assertThat(VersionRequirement.parse("*").satisfies("99.99.99")).isTrue();
    }

    @Test
    void prereleaseShouldRankBelowSameTriple() {
        VersionRequirement req = VersionRequirement.parse(">=1.2.3");
        assertThat(req.satisfies("1.2.3")).isTrue();        // release 恰界含
        assertThat(req.satisfies("1.2.3-rc1")).isFalse();   // 同基段 prerelease 低于界
        assertThat(req.satisfies("1.2.4-rc1")).isTrue();    // 高于界仍满足
        assertThat(VersionRequirement.parse("1.4.2").satisfies("1.4.2-beta")).isFalse(); // 精确不认预发
    }

    @Test
    void shortVersionsShouldPadWithZeros() {
        VersionRequirement req = VersionRequirement.parse("~1.2");
        assertThat(req.satisfies("1.2")).isTrue();       // 1.2 = 1.2.0
        assertThat(req.satisfies("1.2.5")).isTrue();
        assertThat(req.satisfies("1.3")).isFalse();
        assertThat(VersionRequirement.parse("1").satisfies("1.0.0")).isTrue();
    }

    @Test
    void malformedInputsShouldFailFast() {
        assertThatThrownBy(() -> VersionRequirement.parse(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VersionRequirement.parse(" "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> VersionRequirement.parse("^abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("数字");
        VersionRequirement req = VersionRequirement.parse("^1.0");
        assertThatThrownBy(() -> req.satisfies(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> req.satisfies("x.y.z"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

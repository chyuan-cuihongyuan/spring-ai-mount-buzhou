package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.policy.SemVerOrder.Version;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4033 / T6068：语义化版本序合同——官方优先级链全链有序、
 * build 等价、数值段反字典序、数字段 < 字母段、非法五型
 * fail-fast。
 */
class SemVerOrderTest {

    @Test
    void officialPrecedenceChainShouldBeTotallyOrdered() {
        String[] chainTexts = ("1.0.0-alpha,1.0.0-alpha.1,1.0.0-alpha.beta,1.0.0-beta,1.0.0-beta.2,"
                + "1.0.0-beta.11,1.0.0-rc.1,1.0.0").split(",");
        List<Version> chain = Arrays.stream(chainTexts)
                .map(SemVerOrder::parse)
                .toList();
        for (int i = 1; i < chain.size(); i++) {
            assertThat(SemVerOrder.compare(chain.get(i - 1), chain.get(i)))
                    .as("%s < %s", chain.get(i - 1), chain.get(i)).isNegative();
            assertThat(SemVerOrder.compare(chain.get(i), chain.get(i - 1))).isPositive();
        }
        assertThat(chain.get(0).prerelease()).containsExactly("alpha");
    }

    @Test
    void buildMetadataShouldNotAffectPrecedence() {
        Version plain = SemVerOrder.parse("1.0.0");
        Version built = SemVerOrder.parse("1.0.0+20130313144700");
        assertThat(SemVerOrder.compare(plain, built)).isZero();
        assertThat(built.newerThan(plain)).isFalse();
        assertThat(built.build()).isEqualTo("20130313144700");
        assertThat(built.toString()).isEqualTo("1.0.0+20130313144700");
    }

    @Test
    void numericSegmentsShouldDefeatLexicographicOrder() {
        assertThat(SemVerOrder.compare(
                SemVerOrder.parse("1.0.9"), SemVerOrder.parse("1.0.10"))).isNegative();
        assertThat(SemVerOrder.compare(
                SemVerOrder.parse("2.1.0"), SemVerOrder.parse("1.9.9"))).isPositive();
    }

    @Test
    void numericIdentifierShouldRankBelowAlphanumeric() {
        assertThat(SemVerOrder.compare(
                SemVerOrder.parse("1.0.0-1"), SemVerOrder.parse("1.0.0-alpha"))).isNegative();
        assertThat(SemVerOrder.compare(
                SemVerOrder.parse("1.0.0-alpha.1"), SemVerOrder.parse("1.0.0-alpha.beta"))).isNegative();
        assertThat(SemVerOrder.compare(
                SemVerOrder.parse("1.0.0-alpha"), SemVerOrder.parse("1.0.0-alpha.1"))).isNegative();
    }

    @Test
    void malformedVersionsShouldFailFast() {
        assertThatThrownBy(() -> SemVerOrder.parse("1.2")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SemVerOrder.parse("01.0.0")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SemVerOrder.parse("1.0.0-alpha.01")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SemVerOrder.parse("1.0.0-alpha..1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SemVerOrder.parse("1.0.0+α")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SemVerOrder.parse("v1.0.0")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SemVerOrder.parse("1.0.0-")).isInstanceOf(IllegalArgumentException.class);
    }
}

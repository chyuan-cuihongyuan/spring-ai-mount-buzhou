package io.github.chyuan_cuihongyuan.buzhou.skill;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 813 / T1128：技能通道解析回归——latest=最高版本/显式通道优先/
 * 未标定回退 latest/数值段比较免疫字典序陷阱/同通道收敛高版本/脏条目。
 */
class SkillChannelResolverTest {

    private SkillChannelResolver resolver() {
        return new SkillChannelResolver(List.of(
                new SkillChannelResolver.Entry("search", "1.2.0", "latest"),
                new SkillChannelResolver.Entry("search", "1.2.0", "stable"),
                new SkillChannelResolver.Entry("search", "2.0.0-beta", "beta"),
                new SkillChannelResolver.Entry("report", "0.9.0", "stable"),
                new SkillChannelResolver.Entry("report", "0.10.0", "latest")));
    }

    @Test
    void explicitChannelBeatsFallback() {
        assertThat(resolver().resolve("search", "beta")).contains("2.0.0-beta");
        assertThat(resolver().resolve("search", "stable")).contains("1.2.0");
        assertThat(resolver().resolve("search", "latest")).contains("1.2.0");
    }

    @Test
    void untaggedChannelFallsBackToLatest() {
        assertThat(resolver().resolve("search", "canary")).contains("1.2.0");
        assertThat(resolver().resolve("report", "canary")).contains("0.10.0");
    }

    @Test
    void numericSegmentComparisonBeatsLexicographic() {
        // 0.10.0 > 0.9.0（字典序陷阱：\"10\" < \"9\"）
        assertThat(SkillChannelResolver.compareVersions("0.10.0", "0.9.0")).isPositive();
        assertThat(SkillChannelResolver.compareVersions("1.0", "1.0.0")).isZero(); // 短段补 0
        assertThat(SkillChannelResolver.compareVersions("2.0.0-beta", "2.0.0"))
                .isNegative(); // 非数字段字典序兜底
        // latest 缺失时取全表最高
        SkillChannelResolver noLatest = new SkillChannelResolver(List.of(
                new SkillChannelResolver.Entry("x", "0.9.0", "stable"),
                new SkillChannelResolver.Entry("x", "0.10.0", "beta")));
        assertThat(noLatest.resolve("x", "canary")).contains("0.10.0");
    }

    @Test
    void duplicateChannelTagKeepsHigherVersion() {
        SkillChannelResolver collapsed = new SkillChannelResolver(List.of(
                new SkillChannelResolver.Entry("s", "1.0.0", "stable"),
                new SkillChannelResolver.Entry("s", "1.1.0", "stable")));
        assertThat(collapsed.resolve("s", "stable")).contains("1.1.0");
    }

    @Test
    void blankChannelNormalizesToLatestAndDirtyEntriesSkipped() {
        SkillChannelResolver r = new SkillChannelResolver(java.util.Arrays.asList(
                new SkillChannelResolver.Entry("s", "1.0.0", null),
                new SkillChannelResolver.Entry("s", "2.0.0", "  "),
                null,
                new SkillChannelResolver.Entry("", "1.0.0", "latest"),
                new SkillChannelResolver.Entry("ok", null, "latest")));
        assertThat(r.resolve("s", null)).contains("2.0.0"); // 空白通道归 latest，收敛高版本
        assertThat(r.resolve("s", "latest")).contains("2.0.0");
        assertThat(r.names()).containsExactly("s"); // 空名/空版本条目被跳过
        assertThat(r.resolve("ok", "latest")).isEmpty();
        assertThat(r.resolve(null, "latest")).isEmpty();
        assertThat(r.resolve("  ", "latest")).isEmpty();
        assertThat(r.channelsOf("nope")).isEmpty();
    }
}

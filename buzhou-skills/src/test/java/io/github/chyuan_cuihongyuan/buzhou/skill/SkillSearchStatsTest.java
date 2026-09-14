package io.github.chyuan_cuihongyuan.buzhou.skill;

import io.github.chyuan_cuihongyuan.buzhou.skill.classpath.ClasspathSkillScanner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1057 / impl 809：skill_search 搜索判定读面——命中（hits）、零结果（misses，
 * 含语义建议场景）、坏 JSON/空 query 两拒绝桶、四桶守恒恒等式、resetForTest 归零。
 * registry 骨架同 SkillSearchToolTest（classpath 扫描真实技能集，query 动态取名不依赖清单）。
 */
class SkillSearchStatsTest {

    @BeforeEach
    void reset() {
        SkillSearchTool.resetForTest();
    }

    private DefaultSkillRegistry registry() {
        return new DefaultSkillRegistry(cast(new ClasspathSkillScanner().scan()),
                null, null, false, 64);
    }

    private SkillSearchTool tool() {
        return new SkillSearchTool(registry(), new SessionBindingIndex());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static java.util.Map<String, io.github.chyuan_cuihongyuan.buzhou.skill.classpath.ClasspathSkillEntry> cast(
            java.util.Map<String, ?> raw) {
        return (java.util.Map) raw;
    }

    @Test
    void matchingQueryCountsHit() {
        DefaultSkillRegistry registry = registry();
        SkillSearchTool tool = new SkillSearchTool(registry, new SessionBindingIndex());
        // 动态取注册集中首个技能名的子串作 query（不依赖具体清单内容）
        String name = registry.listAllFor(null, null).get(0).name();
        String out = tool.call("{\"query\":\"" + name + "\"}");

        assertThat(out).contains("匹配技能");
        assertThat(SkillSearchTool.stats().hits()).isEqualTo(1);
    }

    @Test
    void zeroResultQueryCountsMiss() {
        String out = tool().call("{\"query\":\"zzzqqqxxx-no-such-skill\"}");
        assertThat(out).contains("无匹配技能");

        assertThat(SkillSearchTool.stats().misses()).isEqualTo(1);
        assertThat(SkillSearchTool.stats().hits()).isZero();
    }

    @Test
    void malformedJsonCountsParseBucket() {
        String out = tool().call("not-json");
        assertThat(out).contains("参数解析失败");

        assertThat(SkillSearchTool.stats().parseRejects()).isEqualTo(1);
    }

    @Test
    void blankQueryCountsItsBucket() {
        String out = tool().call("{}");
        assertThat(out).contains("缺少 query");

        assertThat(SkillSearchTool.stats().blankQueryRejects()).isEqualTo(1);
    }

    @Test
    void conservationIdentityHoldsAcrossMixedCalls() {
        DefaultSkillRegistry registry = registry();
        SkillSearchTool tool = new SkillSearchTool(registry, new SessionBindingIndex());
        String name = registry.listAllFor(null, null).get(0).name();
        tool.call("{\"query\":\"" + name + "\"}");                      // hits
        tool.call("{\"query\":\"zzzqqqxxx-no-such-skill\"}");           // misses
        tool.call("not-json");                                          // parse 拒
        tool.call("{}");                                                // blank 拒

        SkillSearchTool.SkillSearchStats stats = SkillSearchTool.stats();
        assertThat(stats.calls()).isEqualTo(4);
        assertThat(stats.calls())
                .isEqualTo(stats.hits() + stats.misses() + stats.parseRejects()
                        + stats.blankQueryRejects());
        assertThat(stats.hits()).isEqualTo(1);
        assertThat(stats.misses()).isEqualTo(1);
    }

    @Test
    void resetForTestZeroesCounters() {
        SkillSearchTool tool = tool();
        tool.call("{\"query\":\"zzzqqqxxx\"}");
        assertThat(SkillSearchTool.stats().calls()).isEqualTo(1);

        SkillSearchTool.resetForTest();

        SkillSearchTool.SkillSearchStats stats = SkillSearchTool.stats();
        assertThat(stats.calls()).isZero();
        assertThat(stats.misses()).isZero();
    }
}

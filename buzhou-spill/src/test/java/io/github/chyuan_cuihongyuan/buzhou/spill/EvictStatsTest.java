package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1053 / impl 805：evict_handle 逐出判定读面——合法 spill:// 逐出（evictions）、
 * 非 URI 路径与坏 JSON 两拒绝桶、三桶守恒恒等式、resetForTest 归零。
 */
class EvictStatsTest {

    @BeforeEach
    void reset() {
        EvictHandleTool.resetForTest();
    }

    private EvictHandleTool tool() {
        return new EvictHandleTool(new HandleLifecycleRegistry());
    }

    @Test
    void validEvictionCountsEvictions() {
        String out = tool().call("{\"path\":\"spill://agent/s1/tc-1\"}");
        assertThat(out).contains("[已逐出]");

        EvictHandleTool.EvictStats stats = EvictHandleTool.stats();
        assertThat(stats.attempts()).isEqualTo(1);
        assertThat(stats.evictions()).isEqualTo(1);
        assertThat(stats.badPathRejects()).isZero();
        assertThat(stats.parseRejects()).isZero();
    }

    @Test
    void nonUriPathCountsItsBucket() {
        String out = tool().call("{\"path\":\"http://example.com/x\"}");
        assertThat(out).contains("[逐出失败]").contains("spill://");

        EvictHandleTool.EvictStats stats = EvictHandleTool.stats();
        assertThat(stats.badPathRejects()).isEqualTo(1);
        assertThat(stats.evictions()).isZero();
    }

    @Test
    void malformedJsonCountsItsBucket() {
        String out = tool().call("{not-json");
        assertThat(out).contains("[逐出失败]").contains("解析错误");

        EvictHandleTool.EvictStats stats = EvictHandleTool.stats();
        assertThat(stats.parseRejects()).isEqualTo(1);
        assertThat(stats.evictions()).isZero();
    }

    @Test
    void conservationIdentityHoldsAcrossMixedCalls() {
        EvictHandleTool tool = tool();
        tool.call("{\"path\":\"spill://agent/s1/tc-1\"}"); // evictions
        tool.call("{\"path\":\"file:///etc/passwd\"}");    // badPath 拒
        tool.call("garbage");                              // parse 拒
        tool.call("{\"path\":\"spill://agent/s1/tc-2\"}"); // evictions

        EvictHandleTool.EvictStats stats = EvictHandleTool.stats();
        assertThat(stats.attempts()).isEqualTo(4);
        assertThat(stats.attempts())
                .isEqualTo(stats.evictions() + stats.badPathRejects() + stats.parseRejects());
        assertThat(stats.evictions()).isEqualTo(2);
        assertThat(stats.badPathRejects()).isEqualTo(1);
        assertThat(stats.parseRejects()).isEqualTo(1);
    }

    @Test
    void resetForTestZeroesCounters() {
        tool().call("{\"path\":\"spill://agent/s1/tc-9\"}");
        assertThat(EvictHandleTool.stats().attempts()).isEqualTo(1);

        EvictHandleTool.resetForTest();

        EvictHandleTool.EvictStats stats = EvictHandleTool.stats();
        assertThat(stats.attempts()).isZero();
        assertThat(stats.evictions()).isZero();
        assertThat(stats.badPathRejects()).isZero();
        assertThat(stats.parseRejects()).isZero();
    }
}

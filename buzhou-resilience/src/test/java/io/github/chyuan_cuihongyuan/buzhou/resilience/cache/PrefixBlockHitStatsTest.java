package io.github.chyuan_cuihongyuan.buzhou.resilience.cache;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1804 / T2810：前缀块命中账目——跨请求复用、尾块丢弃、请求内去重。 */
class PrefixBlockHitStatsTest {

    /** 共享前缀跨请求命中：相同系统提示的两次请求，共享块全部复用。 */
    @Test
    void sharedPrefixYieldsCrossRequestHits() {
        String sys = "a".repeat(PrefixBlockHitStats.DEFAULT_BLOCK_SIZE)
                + "b".repeat(PrefixBlockHitStats.DEFAULT_BLOCK_SIZE);
        PrefixBlockHitStats.BlockReport report = PrefixBlockHitStats.analyze(
                PrefixBlockHitStats.DEFAULT_BLOCK_SIZE,
                List.of(sys + "问题一", sys + "问题二"));
        // 两次请求各 2 全块：首请求 2 新块，次请求前 2 块复用（尾块「问题二」不足整块丢弃）
        assertThat(report.totalBlocks()).isEqualTo(4);
        assertThat(report.reusedBlocks()).isEqualTo(2);
        assertThat(report.blockHitRatio()).isEqualTo(0.5d);
        assertThat(report.blockMissRatio()).isEqualTo(0.5d);
    }

    /** 尾块不足整块不参与（vLLM partial block 不入缓存）。 */
    @Test
    void trailingPartialBlockIsDiscarded() {
        PrefixBlockHitStats.BlockReport report = PrefixBlockHitStats.analyze(
                4, List.of("12345", "12345"));
        // 每请求仅块 "1234" 入账；尾块 "5" 丢弃
        assertThat(report.totalBlocks()).isEqualTo(2);
        assertThat(report.reusedBlocks()).isEqualTo(1);
    }

    /** 单请求全 miss；同请求内重复块只记首见（不算复用）。 */
    @Test
    void singleRequestAllMissAndIntraRequestDedup() {
        PrefixBlockHitStats.BlockReport single = PrefixBlockHitStats.analyze(
                2, List.of("abcd"));
        assertThat(single.reusedBlocks()).isZero();
        assertThat(single.blockHitRatio()).isZero();

        PrefixBlockHitStats.BlockReport intra = PrefixBlockHitStats.analyze(
                2, List.of("ababab"));
        assertThat(intra.totalBlocks()).isEqualTo(3);
        assertThat(intra.reusedBlocks()).isZero();
    }

    /** 空表、null、全短文本同口径：零入账 + 哨兵 -1。 */
    @Test
    void emptyAndNullYieldSentinels() {
        for (PrefixBlockHitStats.BlockReport report : List.of(
                PrefixBlockHitStats.analyze(4, List.of()),
                PrefixBlockHitStats.analyze(4, null),
                PrefixBlockHitStats.analyze(4, List.of("", "ab")))) {
            assertThat(report.totalBlocks()).isZero();
            assertThat(report.blockHitRatio()).isEqualTo(-1d);
            assertThat(report.blockMissRatio()).isEqualTo(-1d);
        }
    }

    /** 畸形入参 fail-fast：blockSize < 1。 */
    @Test
    void malformedBlockSizeFailsFast() {
        assertThatThrownBy(() -> PrefixBlockHitStats.analyze(0, List.of("x")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blockSize 不能小于 1");
    }

    /** 纯函数性：两次同参调用结果一致（无跨调用状态残留）。 */
    @Test
    void repeatedCallsAreStateless() {
        List<String> prompts = List.of("abcdefgh", "abcdefgh");
        PrefixBlockHitStats.BlockReport first = PrefixBlockHitStats.analyze(4, prompts);
        PrefixBlockHitStats.BlockReport second = PrefixBlockHitStats.analyze(4, prompts);
        assertThat(second).isEqualTo(first);
    }
}

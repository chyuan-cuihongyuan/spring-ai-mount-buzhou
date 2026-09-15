package io.github.chyuan_cuihongyuan.buzhou.spill;

import io.github.chyuan_cuihongyuan.buzhou.core.spi.RangeReadRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * spec 1113 / impl 864：RangeReadEngine 引擎读面——BYTES/JSON/PAGE 三模式
 * 计数、守恒恒等式、resetForTest 归零。
 */
class EngineReadStatsTest {

    @BeforeEach
    void reset() {
        RangeReadEngine.resetForTest();
    }

    @Test
    void eachModeCountsItsBucket() {
        RangeReadEngine.read("内容文本", RangeReadRequest.bytes(0, 4));
        RangeReadEngine.read("{\"k\":1}", RangeReadRequest.json("$.k"));
        RangeReadEngine.read("长内容", RangeReadRequest.page(null, 5));

        RangeReadEngine.EngineReadStats stats = RangeReadEngine.stats();
        assertThat(stats.engineCalls()).isEqualTo(3);
        assertThat(stats.byteReads()).isEqualTo(1);
        assertThat(stats.jsonReads()).isEqualTo(1);
        assertThat(stats.pageReads()).isEqualTo(1);
    }

    @Test
    void conservationIdentityHolds() {
        RangeReadEngine.read("abc", RangeReadRequest.bytes(0, 2));
        RangeReadEngine.read("abc", RangeReadRequest.bytes(1, 2));
        RangeReadEngine.read("{\"a\":1}", RangeReadRequest.json("$"));

        RangeReadEngine.EngineReadStats stats = RangeReadEngine.stats();
        assertThat(stats.engineCalls()).isEqualTo(3);
        assertThat(stats.engineCalls())
                .isEqualTo(stats.byteReads() + stats.jsonReads() + stats.pageReads());
    }

    @Test
    void resetForTestZeroesCounters() {
        RangeReadEngine.read("abc", RangeReadRequest.bytes(0, 1));
        assertThat(RangeReadEngine.stats().engineCalls()).isEqualTo(1);

        RangeReadEngine.resetForTest();

        RangeReadEngine.EngineReadStats stats = RangeReadEngine.stats();
        assertThat(stats.engineCalls()).isZero();
        assertThat(stats.byteReads()).isZero();
        assertThat(stats.jsonReads()).isZero();
        assertThat(stats.pageReads()).isZero();
    }
}

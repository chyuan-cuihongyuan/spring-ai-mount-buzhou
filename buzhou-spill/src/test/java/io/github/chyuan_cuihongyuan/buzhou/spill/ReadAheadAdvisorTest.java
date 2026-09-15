package io.github.chyuan_cuihongyuan.buzhou.spill;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1818 / T2838：预读顾问——尾链检测、指数放大封顶、三态分类。 */
class ReadAheadAdvisorTest {

    /** 顺序链：预读指数放大（2 链=2 倍，4 链=8 倍封顶）。 */
    @Test
    void sequentialChainDoublesReadAhead() {
        ReadAheadAdvisor.Advisory two = ReadAheadAdvisor.advise(1024, List.of(
                new ReadAheadAdvisor.ReadEvent(0, 1024),
                new ReadAheadAdvisor.ReadEvent(1024, 1024)));
        assertThat(two.pattern()).isEqualTo(ReadAheadAdvisor.Pattern.SEQUENTIAL);
        assertThat(two.chainLength()).isEqualTo(2);
        assertThat(two.readAheadBytes()).isEqualTo(2048L);

        ReadAheadAdvisor.Advisory four = ReadAheadAdvisor.advise(1024, List.of(
                new ReadAheadAdvisor.ReadEvent(0, 1024),
                new ReadAheadAdvisor.ReadEvent(1024, 1024),
                new ReadAheadAdvisor.ReadEvent(2048, 1024),
                new ReadAheadAdvisor.ReadEvent(3072, 1024)));
        assertThat(four.chainLength()).isEqualTo(4);
        assertThat(four.readAheadBytes()).isEqualTo(8192L);

        // 链长 8 仍 8 倍（封顶）
        ReadAheadAdvisor.Advisory eight = ReadAheadAdvisor.advise(1024, List.of(
                new ReadAheadAdvisor.ReadEvent(0, 1024),
                new ReadAheadAdvisor.ReadEvent(1024, 1024),
                new ReadAheadAdvisor.ReadEvent(2048, 1024),
                new ReadAheadAdvisor.ReadEvent(3072, 1024),
                new ReadAheadAdvisor.ReadEvent(4096, 1024),
                new ReadAheadAdvisor.ReadEvent(5120, 1024),
                new ReadAheadAdvisor.ReadEvent(6144, 1024),
                new ReadAheadAdvisor.ReadEvent(7168, 1024)));
        assertThat(eight.readAheadBytes()).isEqualTo(8192L);
    }

    /** 跳读：RANDOM 零预读；链在中途断只看尾链。 */
    @Test
    void randomJumpsGetZeroReadAhead() {
        ReadAheadAdvisor.Advisory random = ReadAheadAdvisor.advise(1024, List.of(
                new ReadAheadAdvisor.ReadEvent(0, 1024),
                new ReadAheadAdvisor.ReadEvent(8192, 1024)));
        assertThat(random.pattern()).isEqualTo(ReadAheadAdvisor.Pattern.RANDOM);
        assertThat(random.readAheadBytes()).isZero();

        // 头部链断、尾部链在——尾链判形状
        ReadAheadAdvisor.Advisory tailChain = ReadAheadAdvisor.advise(1024, List.of(
                new ReadAheadAdvisor.ReadEvent(0, 1024),
                new ReadAheadAdvisor.ReadEvent(999999, 512),
                new ReadAheadAdvisor.ReadEvent(1000511, 512)));
        assertThat(tailChain.pattern()).isEqualTo(ReadAheadAdvisor.Pattern.SEQUENTIAL);
        assertThat(tailChain.chainLength()).isEqualTo(2);
    }

    /** 样本不足：COLD 三态零预读。 */
    @Test
    void coldWindowYieldsNoPattern() {
        ReadAheadAdvisor.Advisory single = ReadAheadAdvisor.advise(1024,
                List.of(new ReadAheadAdvisor.ReadEvent(0, 1024)));
        assertThat(single.pattern()).isEqualTo(ReadAheadAdvisor.Pattern.COLD);
        for (ReadAheadAdvisor.Advisory empty : List.of(
                ReadAheadAdvisor.advise(1024, List.of()),
                ReadAheadAdvisor.advise(1024, null))) {
            assertThat(empty.pattern()).isEqualTo(ReadAheadAdvisor.Pattern.COLD);
            assertThat(empty.readAheadBytes()).isZero();
        }
    }

    /** 畸形入参 fail-fast：blockSize < 1、零长读。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> ReadAheadAdvisor.advise(0, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blockSize 不能小于 1");
        assertThatThrownBy(() -> new ReadAheadAdvisor.ReadEvent(0, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("length 不能小于 1");
    }
}

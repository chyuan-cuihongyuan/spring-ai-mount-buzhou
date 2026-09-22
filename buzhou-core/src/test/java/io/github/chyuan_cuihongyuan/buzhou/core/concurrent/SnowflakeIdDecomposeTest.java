package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** spec 1908 / T3018：雪花 ID——roundtrip、分解字段、越界。 */
class SnowflakeIdDecomposeTest {

    private static final long EPOCH = 1_700_000_000_000L;

    /** roundtrip 三例：零序列/满序列 4095/跨机器。 */
    @Test
    void roundTripSelfConsistent() {
        long[][] cases = {
                {1_700_000_001_000L, 7, 0},
                {1_700_000_002_000L, 7, 4095},
                {1_700_000_003_000L, 1023, 42},
        };
        for (long[] c : cases) {
            long id = SnowflakeIdDecompose.compose(c[0], c[1], c[2], EPOCH);
            SnowflakeIdDecompose.Decomposed d =
                    SnowflakeIdDecompose.decompose(id, EPOCH);
            assertThat(d.timestampMillis()).isEqualTo(c[0]);
            assertThat(d.workerId()).isEqualTo(c[1]);
            assertThat(d.sequence()).isEqualTo(c[2]);
        }
    }

    /** 分解字段精确：机器 7 序列 42 的 ID 拆出 7 与 42。 */
    @Test
    void fieldsDecomposePrecisely() {
        SnowflakeIdDecompose.Decomposed d =
                SnowflakeIdDecompose.decompose(
                        SnowflakeIdDecompose.compose(
                                EPOCH + 123456, 7, 42, EPOCH), EPOCH);
        assertThat(d.timestampMillis()).isEqualTo(EPOCH + 123456);
        assertThat(d.workerId()).isEqualTo(7);
        assertThat(d.sequence()).isEqualTo(42);
    }

    /** 时间有序性：后生成的 ID（时戳更大）数值更大。 */
    @Test
    void timeOrdered() {
        long early = SnowflakeIdDecompose.compose(EPOCH + 1, 0, 4095, EPOCH);
        long late = SnowflakeIdDecompose.compose(EPOCH + 2, 0, 0, EPOCH);
        assertThat(late).isGreaterThan(early);
    }

    /** 畸形入参 fail-fast：机器号 1024、序列 4096、时间早于纪元、负 id。 */
    @Test
    void malformedInputFailsFast() {
        assertThatThrownBy(() -> SnowflakeIdDecompose.compose(
                EPOCH, 1024, 0, EPOCH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("机器号须在 [0,1023]");
        assertThatThrownBy(() -> SnowflakeIdDecompose.compose(
                EPOCH, 0, 4096, EPOCH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("序列须在 [0,4095]");
        assertThatThrownBy(() -> SnowflakeIdDecompose.compose(
                EPOCH - 1, 0, 0, EPOCH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("时间戳早于纪元");
        assertThatThrownBy(() -> SnowflakeIdDecompose.decompose(-1, EPOCH))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("id 符号位必须为 0");
    }
}

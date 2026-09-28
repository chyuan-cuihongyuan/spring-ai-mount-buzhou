package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import io.github.chyuan_cuihongyuan.buzhou.core.concurrent.SnowflakeIdGenerator;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;
import java.util.function.LongSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 7036：SnowflakeIdGenerator 合同——分段发号+回拨守卫。
 * 唯一性（批量）；分解往返；时钟回退 fail-fast；机器号隔离；
 * fail-fast。
 */
class SnowflakeIdGeneratorTest {

    @Test
    void batchUniquenessAndStrictIncrease() {
        // 步进时钟（每读 +1ms）——固定时钟会让序列溢出自旋永真（勘误钉住）
        long[] time = {1704067200000L + 100};
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(1, () -> time[0]++);
        Set<Long> seen = new HashSet<>();
        long previous = -1;
        for (int i = 0; i < 5000; i++) {
            long id = gen.nextId();
            assertThat(seen.add(id)).as("id %d 唯一", i).isTrue();
            assertThat(id).isGreaterThan(previous);
            previous = id;
        }
    }

    @Test
    void decomposeRoundTrip() {
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(333, () -> 1704067200000L + 77_000);
        long id = gen.nextId();
        long[] parts = SnowflakeIdGenerator.decompose(id);
        assertThat(parts[1]).isEqualTo(333L);
        assertThat(parts[2]).isZero();
        assertThat(parts[0]).isEqualTo(1704067200000L + 77_000);
    }

    @Test
    void clockRollbackFailsFast() {
        long[] time = {1704067200000L + 5000};
        SnowflakeIdGenerator gen = new SnowflakeIdGenerator(0, () -> time[0]);
        gen.nextId();
        time[0] -= 10;
        assertThatThrownBy(gen::nextId).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void machineIsolationAndFailFast() {
        SnowflakeIdGenerator a = new SnowflakeIdGenerator(7, () -> 1704067200000L);
        SnowflakeIdGenerator b = new SnowflakeIdGenerator(8, () -> 1704067200000L);
        assertThat(a.nextId()).isNotEqualTo(b.nextId());
        assertThatThrownBy(() -> new SnowflakeIdGenerator(1024, () -> 0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SnowflakeIdGenerator(-1, () -> 0L))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SnowflakeIdGenerator(1, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

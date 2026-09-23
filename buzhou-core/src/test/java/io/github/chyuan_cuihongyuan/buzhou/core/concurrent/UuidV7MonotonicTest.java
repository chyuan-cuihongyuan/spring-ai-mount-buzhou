package io.github.chyuan_cuihongyuan.buzhou.core.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 4010 / T6022：UUIDv7 合同——版本变体、时间戳回读、
 * 同毫秒单调、借位有序、畸形 fail-fast。
 */
class UuidV7MonotonicTest {

    @Test
    void versionAndVariantShouldBeRfc9562() {
        UuidV7Monotonic gen = new UuidV7Monotonic(() -> 1_700_000_000_000L, new Random(42));
        for (int i = 0; i < 100; i++) {
            UUID id = gen.next();
            assertThat(id.version()).isEqualTo(7);
            assertThat(id.variant()).isEqualTo(2);   // IETF 变体
            assertThat(UuidV7Monotonic.timestampOf(id)).isEqualTo(1_700_000_000_000L);
        }
    }

    @Test
    void sameMillisShouldStayMonotonicAndDistinct() {
        UuidV7Monotonic gen = new UuidV7Monotonic(() -> 5_000L, new Random(7));
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            ids.add(gen.next());
        }
        assertThat(ids.stream().distinct().count()).isEqualTo(200);   // 随机 lsb 保唯一
        for (int i = 1; i < ids.size(); i++) {
            assertThat(ids.get(i - 1)).isLessThan(ids.get(i));   // 字典序=生成序
        }
        assertThat(ids).allSatisfy(id -> assertThat(UuidV7Monotonic.timestampOf(id)).isEqualTo(5_000L));
    }

    @Test
    void counterOverflowShouldBorrowFromTimestampNotBreakOrder() {
        UuidV7Monotonic gen = new UuidV7Monotonic(() -> 9_000L, new Random(1));
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 5_000; i++) {   // > 4096 计数器容量——必借位
            ids.add(gen.next());
        }
        for (int i = 1; i < ids.size(); i++) {
            assertThat(ids.get(i - 1)).isLessThan(ids.get(i));   // 借位后仍严格有序
        }
        long maxTs = UuidV7Monotonic.timestampOf(ids.get(ids.size() - 1));
        long minTs = UuidV7Monotonic.timestampOf(ids.get(0));
        assertThat(maxTs).isGreaterThan(minTs);   // 借位推进了伪时序
        assertThat(maxTs - minTs).isLessThanOrEqualTo(2);   // 借位幅度受控
    }

    @Test
    void advancingClockShouldBeReflected() {
        long[] tick = {100L};
        UuidV7Monotonic gen = new UuidV7Monotonic(() -> tick[0], new Random(3));
        assertThat(UuidV7Monotonic.timestampOf(gen.next())).isEqualTo(100L);
        tick[0] = 200L;
        assertThat(UuidV7Monotonic.timestampOf(gen.next())).isEqualTo(200L);
        int counter = UuidV7Monotonic.counterOf(gen.next());
        assertThat(counter).isBetween(0, 0xFFF);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new UuidV7Monotonic(null, new Random()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new UuidV7Monotonic(() -> 0L, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UuidV7Monotonic.timestampOf(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> UuidV7Monotonic.counterOf(UUID.randomUUID()))   // v4 拒判
                .isInstanceOf(IllegalArgumentException.class);
    }
}

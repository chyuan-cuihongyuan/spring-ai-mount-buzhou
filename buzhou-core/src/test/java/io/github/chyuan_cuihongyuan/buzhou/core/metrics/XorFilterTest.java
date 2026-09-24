package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5042 / T6186：xor filter 合同——成员无假阴性、
 * 尺寸紧凑界、构建确定性、假阳性率上界、fail-fast。
 */
class XorFilterTest {

    private static final int MEMBER_COUNT = 1000;

    private static final int PROBE_COUNT = 10_000;

    private static final double MAX_FALSE_POSITIVE_RATE = 0.10;

    private static List<Long> memberKeys() {
        List<Long> keys = new ArrayList<>();
        for (long i = 0; i < MEMBER_COUNT; i++) {
            keys.add(i * 7919L);
        }
        return keys;
    }

    @Test
    void membersShouldNeverBeRejected() {
        XorFilter filter = new XorFilter(memberKeys());
        for (long i = 0; i < MEMBER_COUNT; i++) {
            assertThat(filter.contains(i * 7919L))
                    .as("成员 %d 必须命中", i).isTrue();
        }
    }

    @Test
    void arrayLengthShouldBeCompactAndMultipleOfThree() {
        XorFilter filter = new XorFilter(memberKeys());
        assertThat(filter.arrayLength() % 3).isZero();
        long minimum = MEMBER_COUNT * 123L / 100L;
        assertThat(filter.arrayLength()).isGreaterThanOrEqualTo((int) minimum);
        assertThat(filter.elementCount()).isEqualTo(MEMBER_COUNT);
    }

    @Test
    void falsePositiveRateShouldStayUnderBound() {
        XorFilter filter = new XorFilter(memberKeys());
        long positives = 0;
        for (long probe = MEMBER_COUNT; probe < MEMBER_COUNT + PROBE_COUNT; probe++) {
            long key = probe * 7919L;
            if (filter.contains(key)) {
                positives++;
            }
        }
        double rate = (double) positives / PROBE_COUNT;
        assertThat(rate).isLessThan(MAX_FALSE_POSITIVE_RATE);
    }

    @Test
    void sameKeySetShouldBuildIdentically() {
        XorFilter first = new XorFilter(memberKeys());
        XorFilter second = new XorFilter(memberKeys());
        assertThat(first.checksum()).isEqualTo(second.checksum());
        assertThat(first.arrayLength()).isEqualTo(second.arrayLength());
    }

    @Test
    void smallKeySetShouldAlsoBeExactOnMembers() {
        XorFilter filter = new XorFilter(List.of(1L, 2L, 3L, 999999L, -42L));
        assertThat(filter.contains(1L)).isTrue();
        assertThat(filter.contains(2L)).isTrue();
        assertThat(filter.contains(3L)).isTrue();
        assertThat(filter.contains(999999L)).isTrue();
        assertThat(filter.contains(-42L)).isTrue();
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new XorFilter(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new XorFilter(List.of())).isInstanceOf(IllegalArgumentException.class);
        List<Long> withNull = new ArrayList<>();
        withNull.add(null);
        assertThatThrownBy(() -> new XorFilter(withNull)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new XorFilter(List.of(7L, 7L))).isInstanceOf(IllegalArgumentException.class);
    }
}

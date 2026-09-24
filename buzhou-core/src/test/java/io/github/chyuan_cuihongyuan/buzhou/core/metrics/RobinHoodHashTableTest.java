package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5045 / T6192：劫富济贫哈希表合同——换位场景钉住
 * （maxProbeDistance 有界）、后向搬移删除、upsert 覆盖、
 * 100 键回归、fail-fast。
 */
class RobinHoodHashTableTest {

    private static final int SMALL_CAPACITY = 8;

    private static final int BULK_COUNT = 100;

    @Test
    void wraparoundInsertShouldSwapRichForPoor() {
        RobinHoodHashTable<Integer, String> table = new RobinHoodHashTable<>(SMALL_CAPACITY);
        table.put(7, "a");
        table.put(15, "b");
        table.put(0, "c");
        table.put(16, "d");
        table.put(17, "e");
        table.put(24, "f");
        assertThat(table.size()).isEqualTo(6);
        assertThat(table.capacity()).isEqualTo(SMALL_CAPACITY);
        assertThat(table.resizeCount()).isZero();
        assertThat(table.maxProbeDistance()).isEqualTo(3);
        assertThat(table.get(7)).hasValue("a");
        assertThat(table.get(15)).hasValue("b");
        assertThat(table.get(0)).hasValue("c");
        assertThat(table.get(16)).hasValue("d");
        assertThat(table.get(17)).hasValue("e");
        assertThat(table.get(24)).hasValue("f");
        assertThat(table.get(1)).isEmpty();
    }

    @Test
    void removeShouldBackwardShiftAndKeepLookupsIntact() {
        RobinHoodHashTable<Integer, String> table = new RobinHoodHashTable<>(SMALL_CAPACITY);
        table.put(7, "a");
        table.put(15, "b");
        table.put(0, "c");
        table.put(16, "d");
        table.put(17, "e");
        table.put(24, "f");
        assertThat(table.remove(15)).isTrue();
        assertThat(table.remove(7)).isTrue();
        assertThat(table.remove(999)).isFalse();
        assertThat(table.size()).isEqualTo(4);
        assertThat(table.get(0)).hasValue("c");
        assertThat(table.get(16)).hasValue("d");
        assertThat(table.get(17)).hasValue("e");
        assertThat(table.get(24)).hasValue("f");
        assertThat(table.get(15)).isEmpty();
        assertThat(table.get(7)).isEmpty();
    }

    @Test
    void bulkUpsertAndRemoveShouldStayConsistent() {
        RobinHoodHashTable<Integer, String> table = new RobinHoodHashTable<>(16);
        for (int i = 0; i < BULK_COUNT; i++) {
            table.put(i * 13, "v" + i);
        }
        assertThat(table.size()).isEqualTo(BULK_COUNT);
        for (int i = 0; i < BULK_COUNT; i += 2) {
            assertThat(table.put(i * 13, "u" + i)).hasValue("v" + i);
        }
        assertThat(table.size()).isEqualTo(BULK_COUNT);
        for (int i = 1; i < BULK_COUNT; i += 2) {
            assertThat(table.remove(i * 13)).isTrue();
        }
        assertThat(table.size()).isEqualTo(BULK_COUNT / 2);
        for (int i = 0; i < BULK_COUNT; i++) {
            if (i % 2 == 0) {
                assertThat(table.get(i * 13)).hasValue("u" + i);
            } else {
                assertThat(table.get(i * 13)).isEmpty();
            }
        }
        assertThat(table.maxProbeDistance()).isLessThan(BULK_COUNT);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new RobinHoodHashTable<>(0)).isInstanceOf(IllegalArgumentException.class);
        RobinHoodHashTable<String, String> table = new RobinHoodHashTable<>(SMALL_CAPACITY);
        assertThatThrownBy(() -> table.put(null, "v")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> table.put("k", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> table.get(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> table.remove(null)).isInstanceOf(IllegalArgumentException.class);
    }
}

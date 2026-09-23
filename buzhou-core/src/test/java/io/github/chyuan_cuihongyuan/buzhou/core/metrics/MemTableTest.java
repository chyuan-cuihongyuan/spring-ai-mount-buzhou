package io.github.chyuan_cuihongyuan.buzhou.core.metrics;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5024 / T6150：MemTable 合同——upsert、满拒写、drain
 * 字典序导出清空、畸形 fail-fast、确定性。
 */
class MemTableTest {

    @Test
    void putShouldUpsertSameKey() {
        MemTable table = new MemTable(4);
        assertThat(table.put("k", "v1")).isTrue();
        assertThat(table.put("k", "v2")).isTrue();   // upsert 不增位
        assertThat(table.size()).isEqualTo(1);
        assertThat(table.get("k")).isEqualTo("v2");
    }

    @Test
    void fullTableShouldRejectNewKeysButKeepExisting() {
        MemTable table = new MemTable(2);
        table.put("a", "1");
        table.put("b", "2");
        assertThat(table.isFull()).isTrue();
        assertThat(table.put("c", "3")).isFalse();   // 满——拒新
        assertThat(table.put("a", "9")).isTrue();    // 已有键覆盖仍放行
        assertThat(table.get("c")).isNull();
        assertThat(table.get("a")).isEqualTo("9");
    }

    @Test
    void drainShouldExportSortedAndClear() {
        MemTable table = new MemTable(8);
        table.put("c", "3");
        table.put("a", "1");
        table.put("b", "2");
        var exported = table.drain();
        assertThat(exported.keySet()).containsExactly("a", "b", "c");   // 字典序
        assertThat(table.size()).isZero();                              // 清空交接
        assertThat(table.isFull()).isFalse();
        table.put("d", "4");   // 交接后可再写
        assertThat(table.get("d")).isEqualTo("4");
    }

    @Test
    void invalidInputsShouldFailFast() {
        assertThatThrownBy(() -> new MemTable(0)).isInstanceOf(IllegalArgumentException.class);
        MemTable table = new MemTable(2);
        assertThatThrownBy(() -> table.put(null, "v")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> table.put("k", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> table.get(null)).isInstanceOf(IllegalArgumentException.class);
    }
}

package io.github.chyuan_cuihongyuan.buzhou.core.session;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 2016 / T3134：布谷鸟过滤器合同——插入/查询/删除闭环（布伦缺
 * 的删除语义）、删除后可重插、假阳性有界、无假阴性、溢出计数、
 * 畸形 fail-fast。
 */
class CuckooFilterTest {

    @Test
    void insertThenLookupShouldSucceed() {
        CuckooFilter filter = new CuckooFilter(1024);
        filter.insert("session-alpha");
        filter.insert("session-beta");
        assertThat(filter.mightContain("session-alpha")).isTrue(); // 无假阴性
        assertThat(filter.mightContain("session-beta")).isTrue();
        assertThat(filter.size()).isEqualTo(2L);
    }

    @Test
    void deletionShouldRemoveMembership() {
        CuckooFilter filter = new CuckooFilter(1024);
        filter.insert("quarantined-1");
        filter.insert("quarantined-2");
        assertThat(filter.delete("quarantined-1")).isTrue();
        assertThat(filter.mightContain("quarantined-1"))
                .as("删除后指纹离场（布伦做不到的核心差异）").isFalse();
        assertThat(filter.mightContain("quarantined-2")).isTrue(); // 旁员不动
        assertThat(filter.size()).isEqualTo(1L);
    }

    @Test
    void deleteUnknownShouldReturnFalse() {
        CuckooFilter filter = new CuckooFilter(1024);
        assertThat(filter.delete("never-inserted")).isFalse();
        filter.insert("x");
        filter.delete("x");
        assertThat(filter.delete("x")).isFalse(); // 已删再删 false
    }

    @Test
    void reinsertAfterDeleteShouldRestoreMembership() {
        CuckooFilter filter = new CuckooFilter(1024);
        filter.insert("released-session");
        filter.delete("released-session");
        filter.insert("released-session"); // 释放后重新检疫——可重插
        assertThat(filter.mightContain("released-session")).isTrue();
    }

    @Test
    void absentElementsShouldRarelyShowUp() {
        CuckooFilter filter = new CuckooFilter(8192);
        for (int i = 0; i < 1000; i++) {
            filter.insert("member-" + i);
        }
        int falsePositives = 0;
        for (int i = 0; i < 10_000; i++) {
            if (filter.mightContain("nonmember-" + i)) {
                falsePositives++;
            }
        }
        // 16bit 指纹 × 负载 <16%：假阳性率理论上界 ~2×(4×2^-16×8192/8192) 量级——工程宽容 3%
        assertThat((double) falsePositives / 10_000).isLessThan(0.03d);
    }

    @Test
    void capacityOverflowShouldRejectAndCount() {
        // 极小桶（16 桶 × 4 槽 = 64 槽）灌 200 个——必溢出
        CuckooFilter filter = new CuckooFilter(16);
        int inserted = 0;
        for (int i = 0; i < 200; i++) {
            if (filter.insert("flood-" + i)) {
                inserted++;
            }
        }
        assertThat(inserted).isLessThan(200); // 有拒插
        assertThat(filter.overflowCount()).isGreaterThan(0L); // 溢出计数显形
    }

    @Test
    void deterministicBehaviorShouldReplay() {
        CuckooFilter a = new CuckooFilter(1024);
        CuckooFilter b = new CuckooFilter(1024);
        for (int i = 0; i < 300; i++) {
            a.insert("item-" + i);
            b.insert("item-" + i);
        }
        for (int i = 0; i < 300; i += 17) {
            a.delete("item-" + i);
            b.delete("item-" + i);
        }
        // 同序列同答案（无随机踢出）
        for (int i = 0; i < 300; i++) {
            assertThat(a.mightContain("item-" + i)).isEqualTo(b.mightContain("item-" + i));
        }
        assertThat(a.size()).isEqualTo(b.size());
    }

    @Test
    void malformedInputsShouldFailFast() {
        CuckooFilter filter = new CuckooFilter();
        assertThatThrownBy(() -> new CuckooFilter(15))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new CuckooFilter(1000)) // 非 2 的幂
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> filter.insert(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> filter.mightContain(null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> filter.delete(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

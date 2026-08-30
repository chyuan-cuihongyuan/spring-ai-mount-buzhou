package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 157 / T516：弹性预算池回归——保底恒可用 / 借 surplus / 护他人 base /
 * 归还回升 / 构造校验 / 计数。
 */
class ElasticBudgetPoolTest {

    /** 容量 100；a/b base 各 30（surplus 40）。 */
    private static ElasticBudgetPool pool() {
        return new ElasticBudgetPool(100, Map.of("a", 30L, "b", 30L));
    }

    @Test
    void baseQuotaAlwaysAvailableEvenWhenSurpoolBorrowed() {
        ElasticBudgetPool pool = pool();
        // c（无 base）借走全部 surplus 40
        assertThat(pool.tryAcquire("c", 40)).isTrue();
        assertThat(pool.surplus()).isZero();

        // b 的保底 30 仍可取（他人借不走）
        assertThat(pool.tryAcquire("b", 30)).isTrue();
        assertThat(pool.heldOf("b")).isEqualTo(30);
    }

    @Test
    void busySessionBorrowsSurplusBeyondBase() {
        ElasticBudgetPool pool = pool();
        assertThat(pool.tryAcquire("a", 50)).isTrue(); // 30 base + 20 借用
        assertThat(pool.heldOf("a")).isEqualTo(50);
        assertThat(pool.surplus()).isEqualTo(20); // 100 - max(50,30) - 30
        assertThat(pool.borrowedCount()).isEqualTo(1);
    }

    @Test
    void borrowingCannotEatOthersBase() {
        ElasticBudgetPool pool = pool();
        // c 借 60：可借仅 40（护 a/b 各 30 base）→ 拒
        assertThat(pool.tryAcquire("c", 60)).isFalse();
        assertThat(pool.deniedCount()).isEqualTo(1);
        assertThat(pool.tryAcquire("c", 40)).isTrue(); // 顶格借
    }

    @Test
    void releaseRestoresSurplusForReborrow() {
        ElasticBudgetPool pool = pool();
        assertThat(pool.tryAcquire("a", 70)).isTrue(); // base 30 + 借满 surplus 40
        assertThat(pool.surplus()).isZero();
        assertThat(pool.tryAcquire("c", 10)).isFalse();

        pool.release("a", 40); // 归还借用部分
        assertThat(pool.surplus()).isEqualTo(40);
        assertThat(pool.tryAcquire("c", 10)).isTrue();
    }

    @Test
    void constructorValidatesGuaranteeInvariant() {
        assertThatThrownBy(() -> new ElasticBudgetPool(50, Map.of("a", 30L, "b", 30L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("保底");
        assertThatThrownBy(() -> new ElasticBudgetPool(0, Map.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unknownSessionAcquiresViaBorrowOnly() {
        ElasticBudgetPool pool = pool();
        assertThat(pool.tryAcquire("ghost", 40)).isTrue(); // 无 base，纯借 surplus
        assertThat(pool.tryAcquire("ghost", 1)).isFalse(); // surplus 用尽
        assertThat(pool.snapshot().get("ghost").base()).isZero();
    }
}

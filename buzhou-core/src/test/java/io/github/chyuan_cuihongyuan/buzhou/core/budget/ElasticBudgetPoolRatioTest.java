package io.github.chyuan_cuihongyuan.buzhou.core.budget;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 预算池借比例上限测试（spec 624 / T898–T899 / impl 477，K8s LimitRange
 * limit-ratio 思想）：单会话 held ≤ base × ratio；surplus 够也不能越；默认不设限零变化。
 */
class ElasticBudgetPoolRatioTest {

    /** ratio=2：base=100 会话持有至多 200——surplus 充足也拒绝越限。 */
    @Test
    void ratioCapsPerSessionHolding() {
        ElasticBudgetPool pool = new ElasticBudgetPool(1_000,
                Map.of("a", 100L, "b", 100L), 2.0);

        assertThat(pool.tryAcquire("a", 200)).isTrue();   // 恰 base×2
        assertThat(pool.tryAcquire("a", 1)).isFalse();    // 越限拒（surplus 充足）
        assertThat(pool.tryAcquire("b", 150)).isTrue();   // b 不受 a 影响（各自上限）
    }

    /** 默认不设限（null）：surplus 内随便借（既有语义零变化）。 */
    @Test
    void defaultUnlimitedKeepsExistingSemantics() {
        ElasticBudgetPool pool = new ElasticBudgetPool(1_000, Map.of("a", 100L));

        assertThat(pool.tryAcquire("a", 900)).isTrue();   // 吃光全部 surplus——旧语义
        assertThat(pool.tryAcquire("a", 1)).isFalse();    // 容量耗尽才拒
    }

    /** base=0 会话在设 ratio 时不可借（无保底者不可饿死同伴——诚实边界）；校验 ratio<1 拒。 */
    @Test
    void zeroBaseAndValidation() {
        ElasticBudgetPool pool = new ElasticBudgetPool(1_000,
                Map.of("grounded", 100L, "floating", 0L), 2.0);
        assertThat(pool.tryAcquire("grounded", 100)).isTrue();
        assertThat(pool.tryAcquire("floating", 1)).isFalse(); // base=0 × ratio=0 上限

        assertThatThrownBy(() -> new ElasticBudgetPool(100, Map.of(), 0.5))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

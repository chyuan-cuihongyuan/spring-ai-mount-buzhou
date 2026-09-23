package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import io.github.chyuan_cuihongyuan.buzhou.core.policy.DelayScheduling.Locality;
import io.github.chyuan_cuihongyuan.buzhou.core.policy.DelayScheduling.Verdict;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5014 / T6130：延迟调度合同——首选即启、预算等待、ANY
 * 兜底重置、降级梯、畸形 fail-fast。
 */
class DelaySchedulingTest {

    @Test
    void preferredAvailableShouldLaunchImmediately() {
        DelayScheduling scheduling = new DelayScheduling(3);
        Verdict verdict = scheduling.offer(true);
        assertThat(verdict.launch()).isTrue();
        assertThat(verdict.level()).isEqualTo(Locality.PROCESS_LOCAL);
        assertThat(verdict.skipsUsed()).isZero();
    }

    @Test
    void waitingShouldAccumulateThenFallBackToAny() {
        DelayScheduling scheduling = new DelayScheduling(3);
        for (int round = 1; round <= 3; round++) {
            Verdict verdict = scheduling.offer(false);
            assertThat(verdict.launch()).isFalse();
            assertThat(verdict.skipsUsed()).isEqualTo(round);
        }
        assertThat(scheduling.relaxedTo()).isEqualTo(Locality.ANY);   // 三轮降级梯走完
        Verdict fallback = scheduling.offer(false);   // 预算耗尽
        assertThat(fallback.launch()).isTrue();
        assertThat(fallback.level()).isEqualTo(Locality.ANY);   // 本地性换时效
        assertThat(scheduling.skipsUsed()).isZero();   // 重置
    }

    @Test
    void relaunchAfterFallbackShouldPreferLocalAgain() {
        DelayScheduling scheduling = new DelayScheduling(1);
        scheduling.offer(false);            // WAIT（预算 1）
        assertThat(scheduling.offer(false).level()).isEqualTo(Locality.ANY);   // 兜底
        Verdict recovered = scheduling.offer(true);   // 本地槽回来了
        assertThat(recovered.level()).isEqualTo(Locality.PROCESS_LOCAL);   // 重置后重新等本地
    }

    @Test
    void zeroBudgetShouldLaunchAnyImmediately() {
        DelayScheduling scheduling = new DelayScheduling(0);
        Verdict verdict = scheduling.offer(false);
        assertThat(verdict.launch()).isTrue();
        assertThat(verdict.level()).isEqualTo(Locality.ANY);
    }

    @Test
    void negativeBudgetShouldFailFast() {
        assertThatThrownBy(() -> new DelayScheduling(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

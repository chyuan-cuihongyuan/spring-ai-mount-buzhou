package io.github.chyuan_cuihongyuan.buzhou.core.policy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 5027 / T6156：DRR 合同——quantum 记账、亏空结转、队空
 * 清零、容量守恒、畸形 fail-fast。
 */
class DeficitRoundRobinTest {

    private static DeficitRoundRobin newScheduler() {
        return new DeficitRoundRobin(List.of("a", "b"), 10, 100);
    }

    @Test
    void quantumShouldServeFittingHead() {
        DeficitRoundRobin scheduler = newScheduler();
        scheduler.enqueue("a", "a1", 10);
        scheduler.enqueue("b", "b1", 10);
        assertThat(scheduler.dequeue()).isEqualTo("a1");   // a: 10≤10 发出
        assertThat(scheduler.dequeue()).isEqualTo("b1");
        assertThat(scheduler.dequeue()).isNull();          // 单轮无服务——null 不阻塞
    }

    @Test
    void deficitShouldCarryOverForOversizedHead() {
        DeficitRoundRobin scheduler = newScheduler();
        scheduler.enqueue("a", "big", 15);
        scheduler.enqueue("b", "small", 5);
        assertThat(scheduler.dequeue()).isEqualTo("small");   // a 队头 15>10 结转；b 5≤10 发出
        assertThat(scheduler.dequeue()).isEqualTo("big");     // a 亏空 10+10=20 ≥ 15 发出
        assertThat(scheduler.deficitOf("a")).isEqualTo(5);
    }

    @Test
    void emptyQueueShouldResetDeficit() {
        DeficitRoundRobin scheduler = newScheduler();
        scheduler.enqueue("a", "only", 10);
        scheduler.dequeue();   // a 发出，b 队空亏空清零
        assertThat(scheduler.deficitOf("b")).isZero();
    }

    @Test
    void queueCapacityShouldConserve() {
        DeficitRoundRobin scheduler = new DeficitRoundRobin(List.of("a", "b"), 10, 20);
        assertThat(scheduler.enqueue("a", "p1", 15)).isTrue();
        assertThat(scheduler.enqueue("a", "p2", 10)).isFalse();   // 超 20 字节容量
        assertThat(scheduler.queueBytes("a")).isEqualTo(15);
    }

    @Test
    void invalidArgumentsShouldFailFast() {
        assertThatThrownBy(() -> new DeficitRoundRobin(List.of(), 10, 100))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DeficitRoundRobin(List.of("a"), 0, 100))
                .isInstanceOf(IllegalArgumentException.class);
        DeficitRoundRobin scheduler = newScheduler();
        assertThatThrownBy(() -> scheduler.enqueue("ghost", "p", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> scheduler.enqueue("a", "p", 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> scheduler.deficitOf("ghost"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

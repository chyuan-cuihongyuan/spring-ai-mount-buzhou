package io.github.chyuan_cuihongyuan.buzhou.core.backpressure;

import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionCapacityExceededException;
import io.github.chyuan_cuihongyuan.buzhou.core.session.SessionEvent;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * spec 335 / impl-358：spawn 准入地板回归——低于地板即拒（admission-floor
 * 原因事件）/ 地板 HIGH 放行 HIGH / 无地板恒零变化 / FAIL_FAST 档同拒。
 */
class SpawnAdmissionFloorTest {

    private static SpawnGate gate(SpawnAdmissionFloor floor, OverloadPolicy policy) {
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        return new SpawnGate(2, Duration.ofMillis(50), policy, events::add, floor);
    }

    @Test
    void floorHighRejectsNormalAndLow_highPasses() {
        SpawnAdmissionFloor floor = new SpawnAdmissionFloor();
        floor.set(SpawnPriority.HIGH);
        List<SessionEvent> events = new CopyOnWriteArrayList<>();
        SpawnGate gate = new SpawnGate(2, Duration.ofMillis(50), OverloadPolicy.QUEUE,
                events::add, floor);

        assertThatCode(() -> gate.acquireSlotOrThrow("s-high", SpawnPriority.HIGH))
                .doesNotThrowAnyException(); // 运维接管通道冻结期照常
        gate.releaseSlot();
        assertThatThrownBy(() -> gate.acquireSlotOrThrow("s-normal", SpawnPriority.NORMAL))
                .isInstanceOf(SessionCapacityExceededException.class);
        assertThatThrownBy(() -> gate.acquireSlotOrThrow("s-low", SpawnPriority.LOW))
                .isInstanceOf(SessionCapacityExceededException.class);
        assertThat(events).anySatisfy(e -> assertThat(e.type())
                .isEqualTo(SpawnGate.EVENT_SPAWN_REJECTED));
    }

    @Test
    void noFloorBehavesAsBefore_allPrioritiesAdmitted() {
        SpawnGate gate = gate(null, OverloadPolicy.QUEUE); // null = 恒 LOW
        assertThatCode(() -> gate.acquireSlotOrThrow("a", SpawnPriority.LOW))
                .doesNotThrowAnyException();
        assertThatCode(() -> gate.acquireSlotOrThrow("b", SpawnPriority.NORMAL))
                .doesNotThrowAnyException(); // 容量 2 内全放行——存量语义
        gate.releaseSlot();
        gate.releaseSlot();
    }

    @Test
    void defaultFloorLowAdmitsEverything() {
        SpawnGate gate = gate(new SpawnAdmissionFloor(), OverloadPolicy.QUEUE); // 默认 LOW
        assertThatCode(() -> gate.acquireSlotOrThrow("a", SpawnPriority.LOW))
                .doesNotThrowAnyException();
        gate.releaseSlot();
    }

    @Test
    void failFastAlsoHonorsFloor() {
        SpawnAdmissionFloor floor = new SpawnAdmissionFloor();
        floor.set(SpawnPriority.HIGH);
        SpawnGate gate = gate(floor, OverloadPolicy.FAIL_FAST);
        assertThatCode(() -> gate.acquireSlotOrThrow("s-high", SpawnPriority.HIGH))
                .doesNotThrowAnyException();
        gate.releaseSlot();
        assertThatThrownBy(() -> gate.acquireSlotOrThrow("s-low", SpawnPriority.LOW))
                .isInstanceOf(SessionCapacityExceededException.class); // 先于容量判定
    }

    @Test
    void floorLoweredBackLowReadmits() {
        SpawnAdmissionFloor floor = new SpawnAdmissionFloor();
        floor.set(SpawnPriority.HIGH);
        SpawnGate gate = gate(floor, OverloadPolicy.QUEUE);
        floor.set(SpawnPriority.LOW); // 政策解冻
        assertThatCode(() -> gate.acquireSlotOrThrow("s-low", SpawnPriority.LOW))
                .doesNotThrowAnyException();
        gate.releaseSlot();
    }

    @Test
    void floorSlotDefensiveNullFoldsLow() {
        SpawnAdmissionFloor floor = new SpawnAdmissionFloor();
        floor.set(null); // 防御面
        assertThat(floor.get()).isEqualTo(SpawnPriority.LOW);
    }
}
